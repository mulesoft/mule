/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("Can' rely on server socket constructor parameter 'backlog' as its behaviour is implementation dependant")
public class FtpConnectionTimeoutTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort ftpPort = new DynamicPort("ftpPort");

    @Override
    protected String getConfigFile()
    {
        return "ftp-connection-timeout-config-flow.xml";
    }

    @Test
    public void timeoutsConnection() throws Exception
    {
        // Creates a server socket that will accept the first a client and blocking following connections
        ServerSocket serverSocket = new ServerSocket(ftpPort.getNumber(), 1);
        Socket socket = new Socket();
        socket.connect(serverSocket.getLocalSocketAddress());

        try
        {
            MuleClient client = muleContext.getClient();
            MuleMessage result = client.send("vm://in", "somethingFTPFail", null);

            assertThat(result, is(not(nullValue())));
            assertThat(result.getExceptionPayload().getException().getCause().getCause(), instanceOf(SocketTimeoutException.class));
            assertThat(result.getPayload(), instanceOf(NullPayload.class));
        }
        finally
        {
            socket.close();
            serverSocket.close();
        }
    }
}
