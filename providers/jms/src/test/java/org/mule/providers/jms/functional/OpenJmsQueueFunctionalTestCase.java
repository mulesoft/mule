/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms.functional;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
        connector.setProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);        
        //connector.initialise();
        return connector;
    }

    public void testJndiDestinations() throws Exception
    {
        JmsConnector cnn = (JmsConnector)createConnector();
        cnn.initialise();
        Object o = cnn.getJndiContext().lookup("queue1");
        assertNotNull(o);
        assertTrue(o instanceof Destination);

        try
        {
            Object o2 = cnn.getJndiContext().lookup("queue1BlahBlah");
            fail("destination does not exist");
        } catch (NamingException e)
        {
            //expected
        }
    }

}
