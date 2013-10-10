/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.config;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.transport.jms.mulemq.MuleMQJmsConnector;
import org.mule.transport.jms.mulemq.MuleMQXAJmsConnector;
import org.mule.transport.jms.weblogic.WeblogicJmsConnector;
import org.mule.transport.jms.websphere.WebsphereJmsConnector;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the "jms" namespace for vendor-specific configs.
 */
public class JmsVendorNamespaceHandlerTestCase extends FunctionalTestCase
{

    public JmsVendorNamespaceHandlerTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "jms-vendor-namespace-config.xml";
    }

    @Test
    public void testActiveMqDefault() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry()
            .lookupConnector("activeMqConnectorDefaults");
        c.connect();
        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);

        assertNotNull(c.getConnectionFactory());
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals(ActiveMQJmsConnector.DEFAULT_BROKER_URL, ((ActiveMQConnectionFactory) cf).getBrokerURL());
    }

    @Test
    public void testActiveMqBrokerURL() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("activeMqConnectorBroker");
        try
        {
            c.connect();
        }
        catch (Exception e)
        {
            //Connection will fail due there's no broker but the connection factory will be created.
        }
        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);

        assertNotNull(c.getConnectionFactory());
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals("tcp://localhost:1234", ((ActiveMQConnectionFactory) cf).getBrokerURL());
    }

    @Test
    public void testWeblogicDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("weblogicConnector");
        assertNotNull(c);
        assertTrue(c instanceof WeblogicJmsConnector);
    }

    @Test
    public void testWebsphereDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("websphereConnector");
        assertNotNull(c);
        assertTrue(c instanceof WebsphereJmsConnector);
    }

    @Test
    public void testMuleMQDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("muleMqConnector");
        assertNotNull(c);
        assertTrue(c instanceof MuleMQJmsConnector);
        assertEquals("nsp://localhost:9000", ((MuleMQJmsConnector) c).getRealmURL());
        assertEquals(JmsConstants.JMS_SPECIFICATION_11, ((MuleMQJmsConnector) c).getSpecification());
    }

    @Test
    public void testMuleMQBrokerURL() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("muleMqConnectorBroker");
        assertNotNull(c);
        assertTrue(c instanceof MuleMQJmsConnector);
        assertEquals("nsp://localhost:1234", ((MuleMQJmsConnector) c).getRealmURL());
    }

    @Test
    public void testMuleMQXaDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("muleMqXaConnector");
        assertNotNull(c);
        assertTrue(c instanceof MuleMQXAJmsConnector);
        assertEquals("nsp://localhost:9000", ((MuleMQXAJmsConnector) c).getRealmURL());
    }

    @Test
    public void testMuleMQXaBrokerURL() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("muleMqXaConnectorBroker");
        assertNotNull(c);
        assertTrue(c instanceof MuleMQXAJmsConnector);
        assertEquals("nsp://localhost:1234", ((MuleMQJmsConnector) c).getRealmURL());
    }

}
