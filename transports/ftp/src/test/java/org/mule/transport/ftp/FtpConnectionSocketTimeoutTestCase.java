/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;


import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.client.SimpleOptions;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FtpConnectionSocketTimeoutTestCase extends AbstractFtpServerTestCase
{

    public static final String FTP_MESSAGE_RECEIVER_CLASS = "org.mule.transport.ftp.FtpConnectionSocketTimeoutTestCase$TestFtpMessageReceiver";
    private static String CONNECTION_TIMEOUT = "1000";

    private static int TEST_DELTA = 500;

    private static int SERVER_DELTA = 1000;

    private static long WAIT_TIMEOUT = parseLong(CONNECTION_TIMEOUT) + TEST_DELTA;

    private static long SERVER_SLEEP = parseLong(CONNECTION_TIMEOUT) + SERVER_DELTA;

    private static String NOOP_COMMAND = "NOOP";

    private static String ERROR_MESSAGE = "Read timed out";

    @Rule
    public SystemProperty systemProperty = new SystemProperty("connectionTimeout", CONNECTION_TIMEOUT);

    @Rule
    public SystemProperty xmlFtpMessageReceiverSystemProperty = new SystemProperty("ftpMessageReceiverClass", FTP_MESSAGE_RECEIVER_CLASS);

    private Ftplet ftplet;

    private String nameScenario;

    private static Latch latch;

    private static Latch serverLatch;

    private static Exception receiverException;

    public FtpConnectionSocketTimeoutTestCase(Ftplet ftpLet, String nameScenario)
    {
        super(FLOW, "ftp-connection-timeout-config-flow.xml");
        setStartContext(false);
        this.ftplet = ftpLet;
        this.nameScenario = nameScenario;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        receiverException = null;
        latch = new Latch();
        serverLatch = new Latch();
        super.doSetUp();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(new Object[] { ftpLetOnConnectSleep, "Connection Scenario"});
        parameters.add(new Object[] { ftpLetOnCommandSleep, "Connection Commands Scenario"});
        parameters.add(new Object[] { ftpLetOnNoOpCommandSleep, "NOOP Commands Scenario"});
        return parameters;
    }

    @Test
    public void testDispatcherTimeoutConnection() throws Exception
    {
        muleContext.start();
        MuleClient client = muleContext.getClient();
        MuleMessage testMessage = new DefaultMuleMessage("test", new HashMap(), muleContext);
        MuleMessage result = client.send("vm://in", testMessage, new SimpleOptions(WAIT_TIMEOUT));
        assertThat(result, is(notNullValue()));
        assertException(result.getExceptionPayload().getRootException());
    }

    @Test
    public void testReceiverTimeout() throws Exception
    {
        muleContext.start();
        latch.await();
        assertException(getRootCause(receiverException));
    }

    @After
    public void tearDown() throws Exception
    {
        serverLatch.countDown();
    }

    private void assertException(Throwable exception)
    {
        assertThat("An timeout exception should be triggered in the " + nameScenario, exception, is(notNullValue()));
        assertThat("SocketTimeoutException should be triggered in the " + nameScenario, exception.getMessage(), containsString(ERROR_MESSAGE));
    }

    private static Ftplet ftpLetOnCommandSleep = new DefaultFtplet()
    {
        @Override
        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException
        {
            sleep();
            return null;
        }
    };


    private static Ftplet ftpLetOnNoOpCommandSleep = new DefaultFtplet()
    {
        @Override
        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException
        {
            if (NOOP_COMMAND.equals(request.getCommand()))
            {
                sleep();
            }
            return null;
        }
    };

    private static Ftplet ftpLetOnConnectSleep = new DefaultFtplet()
    {
        @Override
        public FtpletResult onConnect(FtpSession session) throws FtpException, IOException
        {
            sleep();
            return null;
        }
    };

    private static void sleep()
    {
        try
        {
            assertThat("Server Latch must not be released until the end of the test", serverLatch.await(SERVER_SLEEP, TimeUnit.MILLISECONDS), is(false));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted exception was triggered", e);
        }
    }

    public static class TestFtpMessageReceiver extends FtpMessageReceiver
    {

        public TestFtpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, long frequency) throws CreateException
        {
            super(connector, flowConstruct, endpoint, frequency);
        }

        @Override
        protected FTPFile[] listFiles() throws Exception
        {
            FTPFile[] files;
            try
            {
                files = super.listFiles();
            }
            catch (Exception e)
            {
                receiverException = e;
                latch.countDown();
                throw e;
            }

            return files;
        }
    }

    @Override
    protected Ftplet createFtpLet()
    {
        return ftplet;
    }
}
