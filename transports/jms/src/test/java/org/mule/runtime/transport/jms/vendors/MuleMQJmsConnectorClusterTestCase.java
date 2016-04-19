/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.vendors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.transport.jms.DefaultJmsTopicResolver;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.JmsTopicResolver;
import org.mule.runtime.transport.jms.mulemq.MuleMQJmsConnector;

import org.junit.Test;

public class MuleMQJmsConnectorClusterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
