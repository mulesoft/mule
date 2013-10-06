/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.api.context.MuleContextAware;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EndpointToEndpointXaTransactionTestCase extends FunctionalTestCase
{

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
        JdbcDatabaseSetUp jdbcDatabaseSetUp = JdbcDatabaseSetUp.createDatabaseOne();
        TransactionScenarios.InboundMessagesGenerator jdbcInboundMessageCreator = jdbcDatabaseSetUp.createInboundMessageCreator();
        TransactionScenarios.OutboundMessagesCounter jdbcOutboundMessagesVerifier = jdbcDatabaseSetUp.createOutboundMessageCreator();
        JdbcDatabaseSetUp jdbcDatabaseSetUp2 = JdbcDatabaseSetUp.createDatabaseTwo();
        TransactionScenarios.OutboundMessagesCounter jdbcOutboundMessagesVerifier2 = jdbcDatabaseSetUp2.createOutboundMessageCreator();
        CompositeTransactionalTestSetUp createTwoDatabasesSetUp = new CompositeTransactionalTestSetUp(jdbcDatabaseSetUp, jdbcDatabaseSetUp2);
        JmsBrokerSetUp createFirstJmsBroker = new JmsBrokerSetUp(port1.getNumber());
        CompositeTransactionalTestSetUp createDatabaseAndJmsBrokerSetUp = new CompositeTransactionalTestSetUp(jdbcDatabaseSetUp, createFirstJmsBroker);
        JmsBrokerSetUp createSecondJmsBroker = new JmsBrokerSetUp(port2.getNumber());
        CompositeTransactionalTestSetUp createTowJmsBrokers = new CompositeTransactionalTestSetUp(createFirstJmsBroker, createSecondJmsBroker);

        return Arrays.asList(new Object[][] {
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //0
                        "org/mule/test/integration/transaction/xa/vm-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //1
                        "org/mule/test/integration/transaction/xa/vm-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //2
                        "org/mule/test/integration/transaction/xa/vm-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()}, //3
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/vm-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, null,
                        new QueueInboundMessageGenerator(), new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //4
                        "org/mule/test/integration/transaction/xa/jms-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, createFirstJmsBroker,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port1.getNumber())},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //5
                        "org/mule/test/integration/transaction/xa/jms-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, createFirstJmsBroker,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port1.getNumber())}, //6
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jms-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, createTowJmsBrokers,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port2.getNumber())},//7
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jms-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, createTowJmsBrokers,
                        new QueueInboundMessageGenerator(), JmsOutboundMessagesCounter.createVerifierForBroker(port2.getNumber())},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //8
                        "org/mule/test/integration/transaction/xa/jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        jdbcInboundMessageCreator, jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //9
                        "org/mule/test/integration/transaction/xa/jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        jdbcInboundMessageCreator, jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //10
                        "org/mule/test/integration/transaction/xa/jdbc-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, createTwoDatabasesSetUp,
                        jdbcInboundMessageCreator, jdbcOutboundMessagesVerifier2},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //11
                        "org/mule/test/integration/transaction/xa/jdbc-different-connectors-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, createTwoDatabasesSetUp,
                        jdbcInboundMessageCreator, jdbcOutboundMessagesVerifier2},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //12
                        "org/mule/test/integration/transaction/xa/jdbc-to-jms-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, createDatabaseAndJmsBrokerSetUp,
                        jdbcInboundMessageCreator, JmsOutboundMessagesCounter.createVerifierForBroker(port1.getNumber())},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //13
                        "org/mule/test/integration/transaction/xa/jdbc-to-jms-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, createDatabaseAndJmsBrokerSetUp,
                        jdbcInboundMessageCreator, JmsOutboundMessagesCounter.createVerifierForBroker(port1.getNumber())},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //14
                        "org/mule/test/integration/transaction/xa/jms-to-jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, createDatabaseAndJmsBrokerSetUp,
                        new QueueInboundMessageGenerator(), jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //15
                        "org/mule/test/integration/transaction/xa/jms-to-jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, createDatabaseAndJmsBrokerSetUp,
                        new QueueInboundMessageGenerator(), jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //16
                        "org/mule/test/integration/transaction/xa/vm-to-jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        new QueueInboundMessageGenerator(), jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //17
                        "org/mule/test/integration/transaction/xa/vm-to-jdbc-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        new QueueInboundMessageGenerator(), jdbcOutboundMessagesVerifier},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //18
                        "org/mule/test/integration/transaction/xa/jdbc-to-vm-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/bitronix-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        jdbcInboundMessageCreator, new QueueOutboundMessagesCounter()},
                {new String[] {"org/mule/test/integration/transaction/xa/xa-transaction-config.xml", //19
                        "org/mule/test/integration/transaction/xa/jdbc-to-vm-xa-transaction-config.xml",
                        "org/mule/test/integration/transaction/xa/jboss-transaction-manager-config.xml"}, jdbcDatabaseSetUp,
                        jdbcInboundMessageCreator, new QueueOutboundMessagesCounter()}
        }
        );
    }

    @Override
    protected String getConfigResources()
    {
        return null;
    }

    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
    }

    @Before
    public void injectMuleContext()
    {
        TransactionManagerServices.getRecoverer().shutdown();
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
        unregisterAllPreviousResources();
        TransactionManagerServices.getConfiguration().setJournal(null);
        if (testSetUp != null)
        {
            testSetUp.initialize();
        }
    }

    private void unregisterAllPreviousResources() throws NoSuchFieldException, IllegalAccessException
    {
        Field resourcesField = ResourceRegistrar.class.getDeclaredField("resources");
        resourcesField.setAccessible(true);
        ((ConcurrentHashMap) resourcesField.get(null)).clear();
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
