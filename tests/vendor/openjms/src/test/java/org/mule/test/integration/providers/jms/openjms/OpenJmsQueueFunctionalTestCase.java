/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.openjms;

import org.mule.providers.jms.JmsConnector;
import org.mule.test.integration.providers.jms.AbstractJmsQueueFunctionalTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class OpenJmsQueueFunctionalTestCase extends AbstractJmsQueueFunctionalTestCase
{
    public Connection getConnection() throws Exception
    {
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.OPEN_JMS_PROPERTIES);
        return JmsTestUtils.getQueueConnection(props);
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();

        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.OPEN_JMS_PROPERTIES);

        connector.setConnectionFactoryJndiName("JmsQueueConnectionFactory");
        connector.setJndiProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        HashMap overrides = new HashMap();
        overrides.put("message.receiver", JmsMessageReceiverSynchronous.class.getName());
        connector.setServiceOverrides(overrides);
        return connector;
    }

    public void testJndiDestinations() throws Exception
    {
        JmsConnector conn = (JmsConnector) createConnector();
        conn.initialise();
        Object o = conn.getJndiContext().lookup("queue1");
        assertNotNull(o);
        assertTrue(o instanceof Destination);

        try {
            conn.getJndiContext().lookup("queue1BlahBlah");
            fail("destination does not exist");
        } catch (NamingException e) {
            // expected
        }
    }

}
