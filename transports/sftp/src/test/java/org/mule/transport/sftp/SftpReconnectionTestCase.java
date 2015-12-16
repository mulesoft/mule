/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MuleException;
import org.mule.context.notification.ConnectionNotification;
import org.mule.tck.listener.ConnectionListener;
import org.mule.tck.listener.FlowExecutionListener;

import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SftpReconnectionTestCase extends AbstractSftpTestCase
{

    private static final long RECONNECTION_FREQUENCY = 1000l;
    private static final String INBOUND_ENDPOINT_DIRECTORY = "data";
    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static final String SFTP_RECEIVING_FLOW_NAME = "receiving";
    //SFTP seems to be very slow in CI. Long timeout provided to avoid flakyness.
    private static final int TIMEOUT = 15000;

    public SftpReconnectionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "mule-sftp-reconnect-test-config.xml"}
        });
    }

    /**
     * Test the sizeCheck feature
     */
    @Test
    public void reconnectStrategy() throws Exception
    {
        ConnectionListener connectionSuccessfulListener = new ConnectionListener(muleContext);

        verifyReconnectionKicksIn();
        startUpSftpEndpoint();
        connectionSuccessfulListener.waitUntilNotificationsAreReceived();
        verifySftpFlowIsRunning();

        stopSftpServerAndClient();
        verifyReconnectionKicksIn();

        connectionSuccessfulListener.reset();
        startUpSftpEndpoint();
        connectionSuccessfulListener.waitUntilNotificationsAreReceived();
        verifySftpFlowIsRunning();
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 9999999;
    }

    private void verifySftpFlowIsRunning() throws Exception
    {
        FlowExecutionListener sftpInboundEndpointFlowExecutionListener = new FlowExecutionListener(SFTP_RECEIVING_FLOW_NAME, muleContext).setTimeoutInMillis(TIMEOUT);
        sftpClient.changeWorkingDirectory(INBOUND_ENDPOINT_DIRECTORY);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        sftpInboundEndpointFlowExecutionListener.waitUntilFlowIsComplete();
    }

    private void startUpSftpEndpoint() throws IOException, MuleException, SftpException
    {
        startSftpServerAndClient();
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    }

    private void verifyReconnectionKicksIn()
    {
        ConnectionListener connectionListener = new ConnectionListener(muleContext);
        connectionListener
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED)
                .setNumberOfExecutionsRequired(2)
                .waitUntilNotificationsAreReceived();
        connectionListener.assertMinimumTimeBetweenNotifications(RECONNECTION_FREQUENCY);
    }

    @Override
    protected boolean startServerOnStartUp()
    {
        return false;
    }
}
