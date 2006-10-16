/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.activemq;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.providers.jms.TransactedJmsMessageReceiver;
import org.mule.test.integration.providers.jms.AbstractJmsTransactionFunctionalTest;
import org.mule.umo.UMOTransactionFactory;

public class ActiveMQJmsTransactionFunctionalTestCase extends AbstractJmsTransactionFunctionalTest
{
    protected ActiveMQConnectionFactory factory = null;

    public ConnectionFactory getConnectionFactory() throws Exception
    {
        if (factory == null)
        {
            factory = new ActiveMQConnectionFactory();
            factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
            factory.setUseEmbeddedBroker(true);
            factory.setBrokerURL("vm://localhost");
            factory.start();
        }
        return factory;
    }

    protected void doTearDown() throws Exception
    {
        factory.stop();
        factory = null;
        super.doTearDown();
    }

    public UMOTransactionFactory getTransactionFactory()
    {
        return new JmsTransactionFactory();
    }

    public JmsConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();
        connector.setSpecification(JmsConstants.JMS_SPECIFICATION_11);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        /** Always use the transacted Jms Message receivers for these test cases */
        Map overrides = new HashMap();
        overrides.put("message.receiver", TransactedJmsMessageReceiver.class.getName());
        connector.setServiceOverrides(overrides);

        // The following comment seems like doesn't count anymore
        /*
         * //Using multiple receiver threads with ActiveMQ causes the DLQ test case
         * to fail //because of ActiveMQ prefetch. Disabling this feature fixes the
         * problem //connector.setCreateMultipleTransactedReceivers(false);
         */
        return connector;
    }

}
