/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.websphere;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.JmsTopicResolver;
import org.mule.tck.AbstractMuleTestCase;

public class WebsphereEmbeddedJmsConnectorTestCase extends AbstractMuleTestCase
{
    public void testConfigurationDefaults()
    {
        WebsphereJmsConnector c = new WebsphereJmsConnector();
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