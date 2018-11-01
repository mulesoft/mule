/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static java.lang.Long.parseLong;
import static org.apache.commons.net.ftp.FTPCmd.LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Mockito.*;
import static org.mule.context.notification.ConnectionNotification.CONNECTION_FAILED;
import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.listener.ConnectionListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPClient;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class FtpClientDestructionOnSocketTimeoutTestCase extends AbstractFtpServerTestCase
{

    public static final String FTP_MESSAGE_RECEIVER_CLASS = "org.mule.transport.ftp.FtpClientDestructionOnSocketTimeoutTestCase$TestFtpMessageReceiver";

    private static String CONNECTION_TIMEOUT = "1000";

    @Rule
    public SystemProperty systemProperty = new SystemProperty("connectionTimeout", CONNECTION_TIMEOUT);

    @Rule
    public SystemProperty xmlAppConfigSystemProperty = new SystemProperty("ftpMessageReceiverClass", FTP_MESSAGE_RECEIVER_CLASS);

    private static int TEST_DELTA = 500;

    private static int SERVER_DELTA = 1000;

    private static long WAIT_TIMEOUT = parseLong(CONNECTION_TIMEOUT) + TEST_DELTA;

    private static long SERVER_SLEEP = parseLong(CONNECTION_TIMEOUT) + SERVER_DELTA;

    private static Latch serverLatch;

    private final String nameScenario;

    private static FtpConnector connectorSpy;

    private boolean ftpClientWasReleased;

    private static AtomicBoolean alreadySentAListCommand = new AtomicBoolean(false);

    public FtpClientDestructionOnSocketTimeoutTestCase(Ftplet ftpLet, String nameScenario)
    {
        super(FLOW, "ftp-connection-timeout-config-flow.xml");
        setStartContext(false);
        this.ftplet = ftpLet;
        this.nameScenario = nameScenario;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        serverLatch = new Latch();
        ftpClientWasReleased = false;
        alreadySentAListCommand.set(false);
        super.doSetUp();
    }

    @After
    public void tearDown() throws Exception
    {
        serverLatch.countDown();
    }

    // Test construction parameters
    @Parameters
    public static Collection<Object[]> data()
    {
        List<Object[]> parameters = new ArrayList<>();
        parameters.add(new Object[] { ftpLetOnListCommandSleep, "LIST Command executed Scenario"});
        return parameters;
    }

    @Test
    public void testDestroyFtpClientOnTimeoutConnection() throws Exception
    {
        muleContext.start();

        // Wait for FtpReceiver to start polling
        assertListCommandIsIssued();

        // Intercept releaseFtp method invocation on FtpConnector object, in order to verify that
        // the FtpClient is destroyed
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // 'releaseFtp' methods second parameter decides whether the FtpClient should be destroyed
                ftpClientWasReleased = invocation.getArgumentAt(2, boolean.class);
                return null;
            }
        }).when(connectorSpy).releaseFtp(any(DefaultInboundEndpoint.class), any(FTPClient.class), anyBoolean());

        // Since already a LIST command was seen, in the second one, the socket timeout should occur
        new ConnectionListener(muleContext)
                .setNumberOfExecutionsRequired(1)
                .setExpectedAction(CONNECTION_FAILED)
                .waitUntilNotificationsAreReceived();

        assertThat(alreadySentAListCommand(), is(true));
        assertThat(ftpClientWasReleased, is(true));
    }

    private void assertListCommandIsIssued()
    {
        PollingProber prober = new PollingProber(WAIT_TIMEOUT, TEST_DELTA);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return FtpClientDestructionOnSocketTimeoutTestCase.alreadySentAListCommand();
            }

            @Override
            public String describeFailure()
            {
                return "No LIST Command was sent.";
            }
        });
    }

    // Proxy FtpMessageReceiver used to be able to intercept incoming messages to the FtpConnector
    public static class TestFtpMessageReceiver extends FtpMessageReceiver
    {
        public TestFtpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, long frequency) throws CreateException
        {
            super(setAndReturnConnectorSpy(connector), flowConstruct, endpoint, frequency);
        }
    }

    // FtpLet used to generate socket timeout on second LIST command reception
    private static Ftplet ftpLetOnListCommandSleep = new DefaultFtplet()
    {
        @Override
        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException
        {
            if (request.getCommand().equals(LIST.getCommand()))
            {
                if (FtpClientDestructionOnSocketTimeoutTestCase.alreadySentAListCommand())
                {
                    sleep();
                }
                else
                {
                    FtpClientDestructionOnSocketTimeoutTestCase.setAlreadySentAListCommand(true);
                }
            }
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


    @Override
    protected Ftplet createFtpLet()
    {
        return ftplet;
    }

    private static Connector setAndReturnConnectorSpy(Connector connector)
    {
        connectorSpy = spy((FtpConnector) connector);
        return connectorSpy;
    }

    public static boolean alreadySentAListCommand()
    {
        return alreadySentAListCommand.get();
    }

    public static void setAlreadySentAListCommand(boolean aBoolean)
    {
        alreadySentAListCommand.set(aBoolean);
    }
}
