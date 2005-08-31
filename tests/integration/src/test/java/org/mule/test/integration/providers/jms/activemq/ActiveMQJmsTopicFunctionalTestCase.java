/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.activemq;

import org.mule.providers.jms.JmsConnector;
import org.mule.test.integration.providers.jms.AbstractJmsQueueFunctionalTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ActiveMQJmsTopicFunctionalTestCase extends AbstractJmsQueueFunctionalTestCase
{
    public Connection getConnection() throws Exception
    {
        // default to ActiveMq for Jms 1.1 support
        Properties p = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);
        return JmsTestUtils.getTopicConnection(p);
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();
        connector.setSpecification(JmsConnector.JMS_SPECIFICATION_11);
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);

        connector.setConnectionFactoryJndiName("JmsTopicConnectionFactory");
        connector.setJndiProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        HashMap overrides = new HashMap();
        overrides.put("message.receiver", JmsMessageReceiverSynchronous.class.getName());
        connector.setServiceOverrides(overrides);
        return connector;
    }

    public boolean useTopics()
    {
        return true;
    }
}
