/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jms.test.TestConnectionFactory;

import javax.jms.ConnectionFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmsConnectionFactoryTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "jms-connection-factory.xml";
    }

    /**
     * Test providerProperties set on JmsConnector are not passed to the underlying
     * ConnectionFactory.
     */
    @Test
    public void testProviderPropertiesNotPassed() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof TestConnectionFactory);
        assertEquals("Provider properties should not be passed to the ConnectionFactory.", "NOT_SET",
            ((TestConnectionFactory)cf).getProviderProperty());
    }

    /**
     * Test connectionFactoryProperties set on JmsConnector are actually passed to
     * the underlying ConnectionFactory.
     */
    @Test
    public void testConnectionFactoryPropertiesPassed() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        ConnectionFactory cf = c.getConnectionFactory();
        assertTrue(cf instanceof TestConnectionFactory);
        assertEquals("ConnectionFactory properties should be passed to the ConnectionFactory.", "TEST_VALUE",
            ((TestConnectionFactory)cf).getConnectionFactoryProperty());
    }
}
