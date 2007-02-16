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

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.providers.jms.TransactedSingleResourceJmsMessageReceiver;
import org.mule.test.integration.providers.jms.AbstractJmsTransactionFunctionalTest;
import org.mule.umo.UMOTransactionFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQJmsSingleResourceTransactionFunctionalTestCase extends
    AbstractJmsTransactionFunctionalTest
{
    protected ActiveMQConnectionFactory factory = null;

    public ConnectionFactory getConnectionFactory() throws Exception
    {
        if (factory == null)
        {
            factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
        }
        return factory;
    }

    protected void doTearDown() throws Exception
    {
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
        overrides.put("message.receiver", TransactedSingleResourceJmsMessageReceiver.class.getName());
        overrides.put("transacted.message.receiver",
            TransactedSingleResourceJmsMessageReceiver.class.getName());

        connector.setServiceOverrides(overrides);
        return connector;
    }

}
