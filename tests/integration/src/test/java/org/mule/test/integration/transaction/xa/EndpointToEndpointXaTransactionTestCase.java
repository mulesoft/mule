/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.api.context.MuleContextAware;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EndpointToEndpointXaTransactionTestCase extends FunctionalTestCase
{

    public static String transactionManagerConfigFile = "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml";
    @ClassRule
    public static DynamicPort port1 = new DynamicPort("port1");
    @ClassRule
    public static DynamicPort port2 = new DynamicPort("port2");

    private final String[] configFiles;
    private final TransactionalTestSetUp testSetUp;
    private final TransactionScenarios.InboundMessagesGenerator inboundMessagesCreator;
    private final TransactionScenarios.OutboundMessagesCounter outboundMessagesCounter;

    public EndpointToEndpointXaTransactionTestCase(String[] configFiles,
                                                   TransactionalTestSetUp testSetUp,
                                                   TransactionScenarios.InboundMessagesGenerator inboundMessagesCreator,
                                                   TransactionScenarios.OutboundMessagesCounter outboundMessagesCounter)
    {
        this.configFiles = configFiles;
        this.testSetUp = testSetUp;
        this.inboundMessagesCreator = inboundMessagesCreator;
        this.outboundMessagesCounter = outboundMessagesCounter;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        JmsBrokerSetUp createFirstJmsBroker = new JmsBrokerSetUp(port1.getNumber());
        JmsBrokerSetUp createSecondJmsBroker = new JmsBrokerSetUp(port2.getNumber());
        CompositeTransactionalTestSetUp createTowJmsBrokers = new CompositeTransactionalTestSetUp(createFirstJmsBroker, createSecondJmsBroker);

        return Arrays.asList(new Object[][] {
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/vm-xa-transaction-config.xml",
                        transactionManagerConfigFile}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/vm-different-connectors-xa-transaction-config.xml",
                        transactionManagerConfigFile}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jms-xa-transaction-config.xml",
                        transactionManagerConfigFile}, createFirstJmsBroker,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port1.getNumber())},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jms-different-connectors-xa-transaction-config.xml",
                        transactionManagerConfigFile}, createTowJmsBrokers,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port2.getNumber())},
        }
        );
    }

    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
    }

    @Before
    public void injectMuleContext()
    {
        if (inboundMessagesCreator instanceof MuleContextAware)
        {
            ((MuleContextAware) inboundMessagesCreator).setMuleContext(muleContext);
        }
        if (outboundMessagesCounter instanceof MuleContextAware)
        {
            ((MuleContextAware) outboundMessagesCounter).setMuleContext(muleContext);
        }
    }

    @Test
    public void allCommit()
    {
        new TransactionScenarios(inboundMessagesCreator, outboundMessagesCounter)
                .testNoFailureDuringFlowExecution();
    }

    @Test
    public void someRollbacksThenCommit()
    {
        new TransactionScenarios(inboundMessagesCreator, outboundMessagesCounter)
                .testIntermittentFailureDuringFlowExecution();
    }

    @Test
    public void allRollbacks()
    {
        new TransactionScenarios(inboundMessagesCreator, outboundMessagesCounter)
                .setVerificationTimeout(1000)
                .testAlwaysFailureDuringFlowException();
    }

    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        if (testSetUp != null)
        {
            testSetUp.initialize();
        }
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        if (testSetUp != null)
        {
            testSetUp.finalice();
        }
    }

}
