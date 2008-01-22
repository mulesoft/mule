/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.config;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.activemq.ActiveMQJmsConnector;
import org.mule.providers.jms.weblogic.WeblogicJmsConnector;
import org.mule.providers.jms.websphere.WebsphereJmsConnector;
import org.mule.tck.FunctionalTestCase;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;


/**
 * Tests the "jms" namespace for vendor-specific configs.
 */
public class JmsVendorNamespaceHandlerTestCase extends FunctionalTestCase
{
    public JmsVendorNamespaceHandlerTestCase()
    {
        setStartContext(false);
    }
    
    protected String getConfigResources()
    {
        return "jms-vendor-namespace-config.xml";
    }

    public void testActiveMqDefault() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("activeMqConnectorDefaults");
        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);

        assertNotNull(c.getConnectionFactory());
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals(ActiveMQJmsConnector.DEFAULT_BROKER_URL, ((ActiveMQConnectionFactory) cf).getBrokerURL());
    }
    
    public void testActiveMqBrokerURL() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("activeMqConnectorBroker");
        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);

        assertNotNull(c.getConnectionFactory());
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals("tcp://localhost:1234", ((ActiveMQConnectionFactory) cf).getBrokerURL());
    }
    
    public void testWeblogicDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("weblogicConnector");
        assertNotNull(c);
        assertTrue(c instanceof WeblogicJmsConnector);
    }

    public void testWebsphereDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("websphereConnector");
        assertNotNull(c);
        assertTrue(c instanceof WebsphereJmsConnector);
    }
}
