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
package org.mule.test.integration.providers.jms;

import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.Session;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JmsClientAcknowledgeTransactionFunctionalTestCase extends AbstractJmsTransactionFunctionalTest
{
    public UMOTransactionFactory getTransactionFactory()
    {
        return new JmsClientAcknowledgeTransactionFactory();
    }

    public Connection getConnection() throws Exception
    {
        return JmsTestUtils.getQueueConnection();
    }

    public UMOConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();

        Properties props = JmsTestUtils.getJmsProperties();
        assertNotNull("Failed to load Jms properyties", props);

        connector.setConnectionFactoryJndiName(props.getProperty("connectionFactoryJNDIName"));
        connector.setJndiProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.setAcknowledgementMode(Session.CLIENT_ACKNOWLEDGE);

        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }

    protected int getAcknowledgementMode()
    {
        return Session.CLIENT_ACKNOWLEDGE;
    }

    public void testSendTransactedAlways() throws Exception
    {
        super.testSendTransactedAlways();
    }

    public void testSendTransactedRollback() throws Exception
    {
        // Rollback not allowed for client acknowledge
    }

    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        // messages are not marked for redelivery in Client Ack mode
    }
}
