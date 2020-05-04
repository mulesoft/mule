/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.mule.api.MuleException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import org.apache.commons.httpclient.HttpVersion;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerStopTestCase extends FunctionalTestCase
{
    private static final int POLL_TIMEOUT_MILLIS = 300;
    private static final int POLL_DELAY_MILLIS = 50;
    private static final String SLOW_PROCESSING_ENDPOINT = "/slow";
    private static final String FAST_PROCESSING_ENDPOINT = "/path";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("listener.port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-stop.xml";
    }

    @After
    public void startMuleContextIfStopped() throws MuleException
    {
        if (muleContext.isStopped())
        {
            muleContext.start();
        }
    }

    @Test
    public void closeClientConnectionsWhenServerIsStopped() throws IOException, MuleException
    {
        try (Socket idlePersistentConnection = generateIdlePersistentConnection())
        {
            muleContext.stop();
            muleContext.start();

            sendRequest(idlePersistentConnection, FAST_PROCESSING_ENDPOINT);
            assertResponse(getResponse(idlePersistentConnection), false);
        }
    }

    @Test
    public void requestInflightDuringShutdownIsRespondedIncludingConnectionCloseHeader() throws IOException, InterruptedException
    {
        Thread stopper = new MuleContextStopper();
        try (Socket slowRequestConnection = new Socket("localhost", dynamicPort.getNumber()))
        {
            sendRequest(slowRequestConnection, SLOW_PROCESSING_ENDPOINT);

            // Stop mule in parallel.
            stopper.start();

            // Response is ok, but connection close header is added.
            String slowRequestResponse = getResponse(slowRequestConnection);
            assertResponse(slowRequestResponse, true);
            assertThat(slowRequestResponse, containsString("Connection: close"));
        }
        finally
        {
            stopper.join();
        }
    }

    @Test
    public void closeIdleConnectionsWhenServerIsStoppedWhileThereIsAnInflightRequest() throws IOException, InterruptedException
    {
        Thread stopper = new MuleContextStopper();
        try (Socket idlePersistentConnection = generateIdlePersistentConnection())
        {
            try (Socket slowRequestConnection = new Socket("localhost", dynamicPort.getNumber()))
            {
                sendRequest(slowRequestConnection, SLOW_PROCESSING_ENDPOINT);

                // Stop mule in parallel.
                stopper.start();

                // The first connection is closed before the second finishes processing.
                new PollingProber(POLL_TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new ConnectionClosedProbe(idlePersistentConnection));
            }
        }
        finally
        {
            stopper.join();
        }
    }

    private Socket generateIdlePersistentConnection() throws IOException
    {
        Socket socket = new Socket("localhost", dynamicPort.getNumber());
        assertThat(socket.isConnected(), is(true));

        sendRequest(socket, FAST_PROCESSING_ENDPOINT);
        assertResponse(getResponse(socket), true);

        sendRequest(socket, FAST_PROCESSING_ENDPOINT);
        assertResponse(getResponse(socket), true);

        return socket;
    }

    private void sendRequest(Socket socket, String endpoint) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(format("GET %s %s", endpoint, HttpVersion.HTTP_1_1));
        writer.println("Host: www.example.com");
        writer.println("");
        writer.flush();
    }

    private String getResponse(Socket socket)
    {
        try (StringWriter writer = new StringWriter())
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!isEmpty(line = reader.readLine()))
            {
                writer.append(line).append("\r\n");
            }
            return writer.toString();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private void assertResponse(String response, boolean shouldBeValid)
    {
        assertThat(isEmpty(response), is(!shouldBeValid));
        if (shouldBeValid)
        {
            assertThat(response, containsString("HTTP/1.1 200"));
        }
    }

    private static class MuleContextStopper extends Thread {
        @Override
        public void run()
        {
            try
            {
                muleContext.stop();
            }
            catch (MuleException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class ConnectionClosedProbe implements Probe
    {

        Socket connection;

        ConnectionClosedProbe(Socket connection)
        {
            this.connection = connection;
        }

        public boolean isSatisfied()
        {
            try
            {
                sendRequest(connection, FAST_PROCESSING_ENDPOINT);
                return isEmpty(getResponse(connection));
            }
            catch (IOException e)
            {
                return true;
            }
        }

        @Override
        public String describeFailure()
        {
            return "An old persistent connection is returning non-empty responses";
        }
    }

}
