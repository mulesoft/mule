/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.vendors;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.jms.DefaultJmsTopicResolver;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsTopicResolver;

public class WebsphereEmbeddedJmsConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "websphere-config.xml";
    }

    public void testDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);

        // TODO has to be confirmed for Websphere
        assertTrue(c.isEagerConsumer());
        JmsTopicResolver resolver = c.getTopicResolver();
        assertNotNull("Topic resolver must not be null.", resolver);
        assertTrue("Wrong topic resolver configured on the connector.",
                   resolver instanceof DefaultJmsTopicResolver);
    }
}
