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

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import java.util.Properties;

/**
 * <code>JmsTopicTransactionFunctionalTest</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class JmsTopicTransactionFunctionalTest extends AbstractJmsTransactionFunctionalTest
{
    public UMOTransactionFactory getTransactionFactory()
    {
        return new JmsTransactionFactory();
    }

    public Connection getConnection() throws Exception
    {
        return JmsTestUtils.getTopicConnection();
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();

        Properties props = JmsTestUtils.getJmsProperties();
        connector.setConnectionFactoryJndiName(props.getProperty("TopicConnectionFactoryJNDIName"));
        connector.setProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);        
        connector.initialise();
        return connector;
    }

    public void afterInitialise() throws Exception
    {
        Thread.sleep(1000);
    }
}