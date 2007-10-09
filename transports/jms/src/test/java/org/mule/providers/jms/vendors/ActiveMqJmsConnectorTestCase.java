/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.vendors;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.activemq.ActiveMqJmsConnector;
import org.mule.tck.FunctionalTestCase;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMqJmsConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "activemq-config.xml";
    }

    public void testConfigurationDefaults() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);

        assertFalse(c.isEagerConsumer());
        
        ConnectionFactory cf = (ConnectionFactory) c.getConnectionFactory().getOrCreate();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals(ActiveMqJmsConnector.BROKER_URL, ((ActiveMQConnectionFactory) cf).getBrokerURL());
        
        assertNotNull(c.getTopicResolver());
        assertTrue("Wrong topic resolver configured on the connector.",
                   c.getTopicResolver() instanceof DefaultJmsTopicResolver);
    }
}