/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;


import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.context.notification.ConnectionNotification;
import org.mule.tck.listener.ConnectionListener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JdbcReconnectionTestCase extends AbstractJdbcFunctionalTestCase
{

    public JdbcReconnectionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "jdbc-reconnection-blocking-config.xml"},
                {ConfigVariant.FLOW, "jdbc-reconnection-nonblocking-config.xml"}});
    }

    @Test
    public void reconnectsAfterConnectException() throws Exception
    {
        MuleClient client = muleContext.getClient();

        // Assert messages are received correctly
        MuleMessage message = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("No message received", message);

        ConnectionListener connectionListener = new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED).setNumberOfExecutionsRequired(3);

        // Stop the database, Mule should try to reconnect
        stopDatabase();

        // Wait for reconnect attempts ("connect failed" notifications)
        connectionListener.waitUntilNotificationsAreReceived();

        // Restart the database
        startDatabase();
        createTable();
        populateTable();

        // Wait for a message to arrive
        message = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Reconnection failed", message);
    }

}
