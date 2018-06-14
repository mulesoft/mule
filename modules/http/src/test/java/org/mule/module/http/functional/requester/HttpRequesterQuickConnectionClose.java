/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpRequesterQuickConnectionClose extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private final TestServerSocket serverSocket = new TestServerSocket(httpPort.getNumber());

    private static int NUMBER_OF_REQUESTS = 50;

    private List<MuleEvent> responses = new ArrayList<>(NUMBER_OF_REQUESTS);

    private List<Throwable> errors = new ArrayList<>(NUMBER_OF_REQUESTS);

    private Thread serverThread;

    @Override
    protected String getConfigFile()
    {
        return "http-request-quick-close-connection-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        serverThread = new Thread(serverSocket);
        serverThread.start();
        serverSocket.getLatch().await();
    }

    @Test
    public void test()
    {
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++)
        {
            MuleEvent response;
            try
            {
                response = runFlow("client");
                responses.add(response);
            }
            catch (Throwable e)
            {
                errors.add(e);
            }
        }
        assertThat(responses.size(), is(NUMBER_OF_REQUESTS));
        assertThat(errors.size(), is(0));
    }

    private class TestServerSocket implements Runnable
    {

        private final int port;
        private final CountDownLatch latch = new CountDownLatch(1);

        public TestServerSocket(int port)
        {
            this.port = port;
        }

        public void run()
        {
            ServerSocket serverSocket = null;

            try
            {
                serverSocket = new ServerSocket(port);
                serverSocket.setReuseAddress(true);
                latch.countDown();
            }
            catch (Exception e)
            {
                // Just Ignore.
            }

            while (!Thread.interrupted())
            {
                try
                {
                    Socket clientSocket = serverSocket.accept();
                    OutputStream os = clientSocket.getOutputStream();
                    os.write("HTTP/1.1 200 Ok".getBytes());
                    os.write("\n".getBytes());
                    os.write("Date: Fri, 09 Mar 2018 09:59:26 GMT".getBytes());
                    os.write("\n".getBytes());
                    os.write("Content-Length: 4".getBytes());
                    os.write("\n".getBytes());
                    os.write("\n".getBytes());
                    os.write(TEST_PAYLOAD.getBytes());
                    os.flush();
                    // With this delay the issue never occurs.
                    //Thread.sleep(500);
                    clientSocket.close();
                }
                catch (Exception e)
                {
                    // Just ignore.
                }
            }
        }

        public CountDownLatch getLatch()
        {
            return latch;
        }
    }
}