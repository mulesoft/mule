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
import org.mule.providers.jms.TransactedJmsMessageReceiver;
import org.mule.providers.jms.activemq.ActiveMqJmsConnector;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.transaction.TransactionManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

public class ActiveMQJmsXaTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
{
    private TransactionManager txManager;

    public ConnectionFactory getConnectionFactory() throws Exception
    {
        if (factory == null)
        {
            factory = new ActiveMQXAConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
        }
        return factory;
    }

    public Connection getSenderConnection() throws Exception
    {
        factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
        return factory.createConnection();
    }

    protected void doSetUp() throws Exception
    {
        // check for already active JOTM instance
        txManager = Current.getCurrent();
        // if none found, create new local JOTM instance
        if (txManager == null)
        {
            new Jotm(true, false);
            txManager = Current.getCurrent();
        }
        txManager.setTransactionTimeout(15000);
        super.doSetUp();
        managementContext.setTransactionManager(txManager);
    }

    public JmsConnector createConnector() throws Exception
    {
        ActiveMqJmsConnector connector = new ActiveMqJmsConnector();
        connector.setName(CONNECTOR_NAME);
        Map overrides = new HashMap();
        overrides.put("transacted.message.receiver", TransactedJmsMessageReceiver.class.getName());
        connector.setServiceOverrides(overrides);
        connector.setSpecification(JmsConstants.JMS_SPECIFICATION_11);
        connector.setConnectionFactoryJndiName("XAJmsQueueConnectionFactory");
        return connector;
    }

    public UMOTransactionFactory getTransactionFactory()
    {
        return new XaTransactionFactory();
    }

    public void afterInitialise() throws Exception
    {
        Thread.sleep(5000);
    }

    public void testSendNotTransacted() throws Exception
    {
        // Cannot send non transacted messages when the connection is an
        // XAConnection
    }

    public void testSendTransactedIfPossibleWithoutTransaction() throws Exception
    {
        // there will always be a transaction available if using an Xa connector
        // so this will always fail
    }
}
