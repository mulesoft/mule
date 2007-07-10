/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.providers.jms.test.TestConnectionFactory;
import org.mule.tck.FunctionalTestCase;

import javax.jms.ConnectionFactory;

public class JmsConnectionFactoryTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "jms-connection-factory.xml";
    }

    /**
     * Test providerProperties set on JmsConnector are not passed to the underlying
     * ConnectionFactory.
     * 
     * TODO This test passes because it doesn't actually do anything, see TODO in jms-connection-factory.xml
     * and mule-jms.xsd
     */
    public void testProviderPropertiesNotPassed() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        ConnectionFactory cf = (ConnectionFactory) c.getConnectionFactory().create();
        assertTrue(cf instanceof TestConnectionFactory);
        assertEquals("Provider properties should not be passed to the ConnectionFactory.", "NOT_SET",
            ((TestConnectionFactory)cf).getProviderProperty());
    }

    /**
     * Test connectionFactoryProperties set on JmsConnector are actually passed to
     * the underlying ConnectionFactory.
     */
    public void testConnectionFactoryPropertiesPassed() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        ConnectionFactory cf = (ConnectionFactory) c.getConnectionFactory().create();
        assertTrue(cf instanceof TestConnectionFactory);
        assertEquals("ConnectionFactory properties should be passed to the ConnectionFactory.", "TEST_VALUE",
            ((TestConnectionFactory)cf).getConnectionFactoryProperty());
    }
}
