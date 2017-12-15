/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.MessagingException;
import org.mule.construct.Flow;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ConnectException;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;

@RunWith(Parameterized.class)
public class HttpRequestProxyConfigTestCase extends FunctionalTestCase
{

    private static Logger LOGGER = getLogger(HttpRequestProxyConfigTestCase.class);
    private static String[] CLOSE_CONNECTION_ERRORS = new String[] {"Connection refused", "Connection reset by peer"};
    private static final String PROXY_HOST = "localhost";
    private static final String PROXY_USERNAME = "theUsername";
    private static final String PROXY_PASSWORD = "thePassword";
    private static final String PROXY_NTLM_DOMAIN = "theNtlmDomain";

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private Thread mockProxyAcceptor;
    private Latch latch = new Latch();
    private Latch proxyReadyLatch = new Latch();

    @Parameter(0)
    public String flowName;

    @Parameter(1)
    public ProxyType proxyType;

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"refAnonymousProxy", ProxyType.ANONYMOUS},
                {"innerAnonymousProxy", ProxyType.ANONYMOUS},
                {"refUserPassProxy", ProxyType.USER_PASS},
                {"innerUserPassProxy", ProxyType.USER_PASS},
                {"refNtlmProxy", ProxyType.NTLM},
                {"innerNtlmProxy", ProxyType.NTLM}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-proxy-config.xml";
    }

    @Before
    public void startMockProxy() throws IOException, InterruptedException
    {
        mockProxyAcceptor = new MockProxy();
        mockProxyAcceptor.start();

        // Give time to the proxy thread to start up completely
        proxyReadyLatch.await();
    }

    @After
    public void stopMockProxy() throws Exception
    {
        mockProxyAcceptor.join(LOCK_TIMEOUT);
    }

    @Test
    public void testProxy() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);

        checkProxyConfig(flow);

        ensureRequestGoesThroughProxy(flow);
    }

    private void checkProxyConfig(Flow flow)
    {
        DefaultHttpRequester httpRequester = (DefaultHttpRequester) flow.getMessageProcessors().get(0);
        ProxyConfig proxyConfig = httpRequester.getConfig().getProxyConfig();

        assertThat(proxyConfig.getHost(), is(PROXY_HOST));
        assertThat(proxyConfig.getPort(), is(Integer.valueOf(proxyPort.getValue())));

        if (proxyType == ProxyType.USER_PASS || proxyType == ProxyType.NTLM)
        {
            assertThat(proxyConfig.getUsername(), is(PROXY_USERNAME));
            assertThat(proxyConfig.getPassword(), is(PROXY_PASSWORD));
            if (proxyType == ProxyType.NTLM)
            {
                assertThat(proxyConfig, is(instanceOf(NtlmProxyConfig.class)));
                assertThat(((NtlmProxyConfig) proxyConfig).getNtlmDomain(), is(PROXY_NTLM_DOMAIN));
            }
        }
    }

    private void ensureRequestGoesThroughProxy(Flow flow) throws Exception
    {
        try
        {
            flow.process(getTestEvent(TEST_MESSAGE));
            fail("Request should go through the proxy.");
        }
        catch (MessagingException e)
        {
            assertThat(e.getCauseException(), is(instanceOf(IOException.class)));
            assertThat(e.getCauseException().getMessage(), isIn(CLOSE_CONNECTION_ERRORS));
        }
        latch.await(1, TimeUnit.SECONDS);
    }

    private enum ProxyType
    {
        ANONYMOUS,
        USER_PASS,
        NTLM
    }

    private class MockProxy extends Thread
    {

        @Override
        public void run()
        {
            ServerSocket serverSocket = null;
            try
            {
                ServerSocketChannel ssc = ServerSocketChannel.open();

                serverSocket = ssc.socket();
                serverSocket.bind(new InetSocketAddress(Integer.parseInt(proxyPort.getValue())));
                ssc.configureBlocking(false);

                proxyReadyLatch.countDown();
                SocketChannel sc = null;
                while (sc == null)
                {
                    sc = ssc.accept();
                    Thread.yield();
                }

                try
                {
                    sleep(100);
                }
                catch (InterruptedException e)
                {
                    //Ignore exception
                }

                sc.close();

                latch.release();
            }
            catch (IOException e)
            {
                /* Ignore */
                LOGGER.error("Exception while configuring MockProxy", e);
            }
            finally
            {
                if (serverSocket != null)
                {
                    try
                    {
                        serverSocket.close();
                    }
                    catch (IOException e)
                    {
                        /* Ignore */
                        LOGGER.error("Exception while closing MockProxy", e);
                    }
                }
            }
        }
    }
}
