/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.vendors;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jms.DefaultJmsTopicResolver;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.transport.jms.redelivery.JmsXRedeliveryHandler;
import org.mule.transport.jms.test.TestRedeliveryHandler;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActiveMQJmsConnectorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/activemq-config.xml";
    }

    @Test
    public void testConfigurationDefaults() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);

        assertFalse(c.isEagerConsumer());
        
        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals(ActiveMQJmsConnector.DEFAULT_BROKER_URL, ((ActiveMQConnectionFactory) cf).getBrokerURL());
        
        assertNotNull(c.getTopicResolver());
        assertTrue("Wrong topic resolver configured on the connector.",
                   c.getTopicResolver() instanceof DefaultJmsTopicResolver);
    }
    
    @Test
    public void testDefaultActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("activeMqJmsConnector");

        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);
        
        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory() instanceof ActiveMQConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof JmsXRedeliveryHandler);
        
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertFalse(c.isCacheJmsSessions());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.0.2b", c.getSpecification());
    }
    
    @Test
    public void testCustomActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("customActiveMqJmsConnector");

        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);
        
        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory() instanceof ActiveMQConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);

        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }
}
