package net.corda.examples.obligation

import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.setCordappPackages
import net.corda.testing.unsetCordappPackages
import java.util.*

open class ObligationTests {
    lateinit var net: net.corda.testing.node.MockNetwork
    lateinit var a: net.corda.node.internal.StartedNode<MockNetwork.MockNode>
    lateinit var b: net.corda.node.internal.StartedNode<MockNetwork.MockNode>
    lateinit var c: net.corda.node.internal.StartedNode<MockNetwork.MockNode>

    @org.junit.Before
    fun setup() {
        setCordappPackages("net.corda.examples.obligation", "net.corda.finance")

        net = MockNetwork(threadPerNode = true)

        val nodes = net.createSomeNodes(3)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        c = nodes.partyNodes[2]

        nodes.partyNodes.forEach {
            it.registerInitiatedFlow(IssueObligation.Responder::class.java)
            it.registerInitiatedFlow(TransferObligation.Responder::class.java)
        }
    }

    @org.junit.After
    fun tearDown() {
        net.stopNodes()
        unsetCordappPackages()
    }

    protected fun issueObligation(borrower: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                  lender: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                  amount: net.corda.core.contracts.Amount<Currency>,
                                  anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val lenderIdentity = lender.info.chooseIdentity()
        val flow = IssueObligation.Initiator(amount, lenderIdentity, anonymous)
        return borrower.services.startFlow(flow).resultFuture.getOrThrow()
    }

    protected fun transferObligation(linearId: net.corda.core.contracts.UniqueIdentifier,
                                     lender: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                     newLender: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                     anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val newLenderIdentity = newLender.info.chooseIdentity()
        val flow = TransferObligation.Initiator(linearId, newLenderIdentity, anonymous)
        return lender.services.startFlow(flow).resultFuture.getOrThrow()
    }

    protected fun settleObligation(linearId: net.corda.core.contracts.UniqueIdentifier,
                                   borrower: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                   amount: net.corda.core.contracts.Amount<Currency>,
                                   anonymous: Boolean = true
    ): net.corda.core.transactions.SignedTransaction {
        val flow = SettleObligation.Initiator(linearId, amount, anonymous)
        return borrower.services.startFlow(flow).resultFuture.getOrThrow()
    }

    protected fun selfIssueCash(party: net.corda.node.internal.StartedNode<MockNetwork.MockNode>,
                                amount: net.corda.core.contracts.Amount<Currency>): net.corda.core.transactions.SignedTransaction {
        val notary = party.services.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.Companion.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(amount, issueRef, notary)
        val flow = CashIssueFlow(issueRequest)
        return party.services.startFlow(flow).resultFuture.getOrThrow().stx
    }
}
