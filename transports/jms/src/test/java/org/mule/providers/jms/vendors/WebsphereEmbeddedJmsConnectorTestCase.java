/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.vendors;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsTopicResolver;
import org.mule.tck.FunctionalTestCase;

public class WebsphereEmbeddedJmsConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "websphere-config.xml";
    }

    public void testDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);

        // TODO has to be confirmed for Websphere
        assertTrue(c.isEagerConsumer());
        assertFalse("JMS connection recovery is not supported by Websphere Embedded provider.",
                    c.isRecoverJmsConnections());
        JmsTopicResolver resolver = c.getTopicResolver();
        assertNotNull("Topic resolver must not be null.", resolver);
        assertTrue("Wrong topic resolver configured on the connector.",
                   resolver instanceof DefaultJmsTopicResolver);
    }
}