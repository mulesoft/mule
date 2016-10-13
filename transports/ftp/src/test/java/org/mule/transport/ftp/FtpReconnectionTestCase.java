/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.exception.AbstractSystemExceptionStrategy;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.ConnectException;
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
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Verify that some Connect Exceptions-like are being handled and result in reconnections.
 */
public class FtpReconnectionTestCase extends AbstractFtpServerTestCase
{
    private TestSystemExceptionStrategy tryReconnectionStrategy;
    MockFailingServer server;

    public FtpReconnectionTestCase (ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Rule
    public DynamicPort listenPort = new DynamicPort("port1");


    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-reconnection.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        createFtpServerBaseDir();

        tryReconnectionStrategy = new TestSystemExceptionStrategy(muleContext);
        muleContext.setExceptionListener(tryReconnectionStrategy);

        server = new MockFailingServer();
        server.start();
        createFtpServerDir("lostConnection");
    }

    @Override
    protected void doTearDown() throws Exception
    {
        server.stop();
        deleteFtpServerBaseDir();
    }

    @Test
    public void testLostConnection() throws Exception
    {
        createFileOnFtpServer("lostConnection/test1");
        new PollingProber(5000, 50).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return tryReconnectionStrategy.reconnect;
            }

            @Override
            public String describeFailure()
            {
                return "Should try to reconnect";
            }
        });
    }

    private class TestSystemExceptionStrategy extends AbstractSystemExceptionStrategy
    {
        public boolean reconnect = false;
        public TestSystemExceptionStrategy(MuleContext muleContext)
        {
            super(muleContext);
        }

        protected void handleReconnection(ConnectException ex)
        {
            reconnect = true;
        }
    }

    private class MockFailingServer implements Runnable
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

                reader.close();
                // client reads null message
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
    }

}
