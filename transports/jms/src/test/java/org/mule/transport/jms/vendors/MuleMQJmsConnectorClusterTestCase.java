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
import org.mule.transport.jms.JmsTopicResolver;
import org.mule.transport.jms.mulemq.MuleMQJmsConnector;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MuleMQJmsConnectorClusterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "mulemq-cluster-config.xml";
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);
        assertTrue(c instanceof MuleMQJmsConnector);
        MuleMQJmsConnector mqc = (MuleMQJmsConnector)c;
        assertTrue(mqc.isInCluster());
        assertTrue(c.isEagerConsumer());
        JmsTopicResolver resolver = c.getTopicResolver();
        assertNotNull("Topic resolver must not be null.", resolver);
        assertTrue("Wrong topic resolver configured on the connector.",
                   resolver instanceof DefaultJmsTopicResolver);   
    }
}
