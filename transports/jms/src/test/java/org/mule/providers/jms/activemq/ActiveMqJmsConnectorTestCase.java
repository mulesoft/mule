/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.activemq;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.JmsTopicResolver;
import org.mule.providers.jms.activemq.ActiveMqJmsConnector;
import org.mule.tck.AbstractMuleTestCase;

public class ActiveMqJmsConnectorTestCase extends AbstractMuleTestCase
{
    public void testConfigurationDefaults()
    {
        ActiveMqJmsConnector c = new ActiveMqJmsConnector();
        assertFalse(c.isEagerConsumer());
        JmsTopicResolver resolver = c.getTopicResolver();
        assertNotNull("Topic resolver must not be null.", resolver);
        assertTrue("Wrong topic resolver configured on the connector.",
                   resolver instanceof DefaultJmsTopicResolver);
    }
}