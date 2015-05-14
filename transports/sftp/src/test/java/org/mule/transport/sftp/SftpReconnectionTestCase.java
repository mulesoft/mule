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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SftpReconnectionTestCase extends AbstractSftpTestCase
{

    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static final String SFTP_RECEIVING_FLOW_NAME = "receiving";
    private static final long TIMEOUT = 15000;
    private static final int FILE_SIZE = 1024 * 1024 * 2;

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

    private void verifySftpFlowIsRunning() throws Exception
    {
        FlowExecutionListener sftpInboundEndpointFlowExecutionListener = new FlowExecutionListener(SFTP_RECEIVING_FLOW_NAME, muleContext);
        executeBaseTest(INBOUND_ENDPOINT_NAME, "vm://test.upload", FILENAME, FILE_SIZE, SFTP_RECEIVING_FLOW_NAME, TIMEOUT);
        sftpInboundEndpointFlowExecutionListener.waitUntilFlowIsComplete();
    }

    private void startUpSftpEndpoint() throws IOException, MuleException, SftpException
    {
        startSftpServerAndClient();
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    }

    private void verifyReconnectionKicksIn()
    {
        new ConnectionListener(muleContext)
                .setExpectedAction(ConnectionNotification.CONNECTION_FAILED)
                .setNumberOfExecutionsRequired(2)
                .waitUntilNotificationsAreReceived();
    }

    @Override
    protected boolean startServerOnStartUp()
    {
        return false;
    }
}
