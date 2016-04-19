/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.vendors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.transport.jms.DefaultJmsTopicResolver;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.runtime.transport.jms.redelivery.JmsXRedeliveryHandler;
import org.mule.runtime.transport.jms.test.TestRedeliveryHandler;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.springframework.jms.connection.CachingConnectionFactory;

public class ActiveMQJmsConnectorTestCase extends FunctionalTestCase
{

    private static String USERNAME = "username";
    private static String PASSWORD = "password";

    @Override
    protected String getConfigFile()
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
        assertTrue(c.isCacheJmsSessions());
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
        assertTrue(c.getConnectionFactory() instanceof CachingConnectionFactory);
        assertTrue(((CachingConnectionFactory) c.getConnectionFactory()).getTargetConnectionFactory() instanceof ActiveMQConnectionFactory);
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

    /**
     * See MULE-8221
     */
    @Test
    public void testActiveMqConnectorWithUsernameAndPassword() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("activeMqJmsConnectorWithUsernameAndPassword");

        assertTrue(c instanceof ActiveMQJmsConnector);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

        assertEquals(USERNAME, c.getUsername());
        assertEquals(PASSWORD, c.getPassword());
        assertTrue(c.isCacheJmsSessions());
        assertEquals("1.1", c.getSpecification());
    }

}
