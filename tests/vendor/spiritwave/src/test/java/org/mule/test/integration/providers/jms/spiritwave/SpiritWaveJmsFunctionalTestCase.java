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
package org.mule.test.integration.providers.jms.spiritwave;

import org.mule.providers.jms.JmsConnector;
import org.mule.test.integration.providers.jms.AbstractJmsFunctionalTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SpiritWaveJmsFunctionalTestCase extends AbstractJmsFunctionalTestCase
{
    public Connection getConnection() throws Exception
    {
        Properties p = JmsTestUtils.getJmsProperties(JmsTestUtils.SPIRIT_WAVE_JMS_PROPERTIES);
        return JmsTestUtils.getQueueConnection(p);
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.SPIRIT_WAVE_JMS_PROPERTIES);

        connector.setConnectionFactoryJndiName("JmsQueueConnectionFactory");
        connector.setJndiProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        return connector;
    }
}
