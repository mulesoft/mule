/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.test.TestConnectionFactory;

import javax.jms.ConnectionFactory;

import org.junit.Test;

public class JmsConnectionFactoryTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
