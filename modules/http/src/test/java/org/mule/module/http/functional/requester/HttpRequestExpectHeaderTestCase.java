/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.EXPECTATION_FAILED;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.EXPECT;
import static org.mule.module.http.api.HttpHeaders.Values.CONTINUE;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;
import org.mule.util.concurrent.Latch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;


public class HttpRequestExpectHeaderTestCase extends FunctionalTestCase
{

    private static final String CONTINUE_RESPONSE = "HTTP/1.1 100 Continue\r\n\r\n";
    private static final String EXPECTATION_FAILED_RESPONSE = "HTTP/1.1 417 Expectation Failed\r\n";
    private static final String REQUEST_FLOW_NAME = "requestFlow";

    @Rule
    public DynamicPort listenPort = new DynamicPort("httpPort");

    private String requestBody;

    @Override
    protected String getConfigFile()
    {
        return "http-request-expect-header-config.xml";
    }

    @Test
    public void handlesContinueResponse() throws Exception
    {
        ExpectContinueMockServer server = new ExpectContinueMockServer();
        server.start();

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.getMessage().setOutboundProperty(EXPECT, CONTINUE);

        flow.process(event);
        assertThat(requestBody, equalTo(TEST_MESSAGE));

        server.stop();
    }

    @Test
    public void handlesExpectationFailedResponse() throws Exception
    {
        ExpectFailedMockServer server = new ExpectFailedMockServer();
        server.start();

        Flow flow = (Flow) getFlowConstruct(REQUEST_FLOW_NAME);

        // Set a payload that will fail when consumed. As the server rejects the request after processing
        // the header, the client should not send the body.

        MuleEvent event = getTestEvent(new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("Payload should not be consumed");
            }
        });
        event.getMessage().setOutboundProperty(EXPECT, CONTINUE);

        MuleEvent response = flow.process(event);
        assertThat(response.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY),
                   equalTo(EXPECTATION_FAILED.getStatusCode()));

        server.stop();
    }

    private abstract class AbstractMockServer implements Runnable
    {

        private Latch startedLatch = new Latch();
        private Latch finishedLatch = new Latch();

        public void start()
        {
            try
            {
                Thread serverThread = new Thread(this);
                serverThread.start();
                startedLatch.await();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("Failed to start mock server", e);
            }
        }

        public void stop()
        {
            try
            {
                finishedLatch.await();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("Failed to stop mock server", e);
            }
        }

        @Override
        public void run()
        {
            try
            {
                ServerSocket serverSocket = new ServerSocket(listenPort.getNumber());
                startedLatch.release();
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream), 1);

                while (!reader.readLine().isEmpty())
                {
                    // Do nothing, consume headers until blank line.
                }

                process(reader, writer);

                reader.close();
                writer.close();
                socket.close();
                serverSocket.close();

                finishedLatch.release();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        protected abstract void process(BufferedReader reader, BufferedWriter writer) throws IOException;
    }


    private class ExpectContinueMockServer extends AbstractMockServer
    {

        @Override
        protected void process(BufferedReader reader, BufferedWriter writer) throws IOException
        {
            writer.write(CONTINUE_RESPONSE);
            writer.flush();

            char[] body = new char[TEST_MESSAGE.length()];
            IOUtils.read(reader, body);
            requestBody = new String(body);

            String response = "HTTP/1.1 200 OK\n" +
                              "Content-Length: 4\n\n" +
                              "TEST";

            writer.write(response);
            writer.flush();
        }
    }


    private class ExpectFailedMockServer extends AbstractMockServer
    {

        @Override
        protected void process(BufferedReader reader, BufferedWriter writer) throws IOException
        {
            writer.write(EXPECTATION_FAILED_RESPONSE);
            writer.flush();
        }
    }
}
