/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.jms.functional;

import org.mule.config.MuleProperties;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.XaJmsMessageReceiver;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ActiveMQJmsXaTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        //As there is no default tx manager impl shipped with the core distribution
        //this test cannot currently run!  Need to move it to the integration test suite
        //MuleManager.getInstance().setTransactionManager(new JotmTransactionManagerFactory().create());
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();
        connector.setSpecification(JmsConnector.JMS_SPECIFICATION_11);
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);

        connector.setConnectionFactoryJndiName("XAJmsQueueConnectionFactory");
        connector.setProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.setDisposeDispatcherOnCompletion(true);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        Map serviceOverrides = new HashMap();
        serviceOverrides.put(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, XaJmsMessageReceiver.class.getName());
        connector.setServiceOverrides(serviceOverrides);
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
        //Cannot send non transacted messages when the connection is an XAConnection
    }

    public void testSendTransactedIfPossibleWithoutTransaction() throws Exception
    {
        //there will always be a transaction available if using an Xa connector
        //so this will always fail
    }
}