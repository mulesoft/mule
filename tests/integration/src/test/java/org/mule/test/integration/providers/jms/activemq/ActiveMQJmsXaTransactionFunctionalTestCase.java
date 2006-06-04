/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.integration.providers.jms.activemq;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.ActiveMQXAConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.mule.MuleManager;
import org.mule.providers.jms.JmsConnector;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class ActiveMQJmsXaTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
{
    private TransactionManager txManager;

    public ConnectionFactory getConnectionFactory() throws Exception
    {
        if(factory==null) {
            factory = new ActiveMQXAConnectionFactory();
            factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
            factory.setUseEmbeddedBroker(true);
            factory.setBrokerURL("vm://localhost");
            factory.start();
        }
        return factory;
    }

    public Connection getSenderConnection() throws Exception
    {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
        factory.setUseEmbeddedBroker(true);
        factory.setBrokerURL("vm://localhost");
        return factory.createConnection();
    }

    protected void doSetUp() throws Exception
    {
        // check for already active JOTM instance
        txManager = Current.getCurrent();
        // if none found, create new local JOTM instance
        if (txManager == null) {
            new Jotm(true, false);
            txManager = Current.getCurrent();
        }
        txManager.setTransactionTimeout(15000);
        super.doSetUp();
        MuleManager.getInstance().setTransactionManager(txManager);
    }

    public JmsConnector createConnector() throws Exception
    {
        JmsConnector connector = super.createConnector();
        connector.setConnectionFactoryJndiName("XAJmsQueueConnectionFactory");
        return connector;
    }

    public UMOTransactionFactory getTransactionFactory()
    {
        return new XaTransactionFactory();
    }

    public void afterInitialise() throws Exception
    {
        Thread.sleep(20000);
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
