/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.activemq;


import org.mule.providers.jms.JmsConnector;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.provider.UMOConnector;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class ActiveMQJmsXaTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
{
    private TransactionManager txManager;

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
        managementContext.setTransactionManager(txManager);
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = (JmsConnector) super.createConnector();
        connector.setConnectionFactoryJndiName("XAJmsQueueConnectionFactory");
        return connector;
    }

    public UMOTransactionFactory getTransactionFactory()
    {
        return new XaTransactionFactory();
    }

    public void afterInitialise() throws Exception
    {
        Thread.sleep(2000);
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
