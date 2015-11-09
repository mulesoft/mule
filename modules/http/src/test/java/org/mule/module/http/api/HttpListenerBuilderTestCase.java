/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.context.WorkManagerSource;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.execution.MessageProcessingManager;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerBuilder;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.http.internal.listener.HttpListenerConnectionManager;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.api.TlsContextFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class HttpListenerBuilderTestCase extends AbstractMuleTestCase
{

    public static URL TEST_URL;
    public static final String PATH = "somePath";
    public static final int PORT = 1000;
    public static final String HOST = "localhost";
    public static final String IP = "127.0.0.1";

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
    private DefaultHttpListenerConfig mockListenerConfig = mock(DefaultHttpListenerConfig.class);
    private Flow mockFlow = mock(Flow.class);
    private MessageProcessingManager mockMessageProcessingManager = mock(MessageProcessingManager.class);
    private HttpListenerConnectionManager mockListenerConnectionManager = mock(HttpListenerConnectionManager.class, Answers.RETURNS_DEEP_STUBS.get());

    static
    {
        try
        {
            TEST_URL = new URL("http://localhost:1010/path");
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetPortAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setUrl(TEST_URL).setPort(PORT);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetHostAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setUrl(TEST_URL).setHost(HOST);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetPathAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setUrl(TEST_URL).setPath(PATH);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrlAfterSetHost() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setPort(PORT).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrltAfterSetPort() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setHost(HOST).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrlAfterSetPath() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setPath(PATH).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetTlsContextIfProtocolIsHttp() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setUrl(TEST_URL).setTlsContextFactory(mockTlsContextFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetTlsContextIfThereIsAListenerConfig() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setListenerConfig(mockListenerConfig).setTlsContextFactory(mockTlsContextFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetAListenerConfigIfThereIsATlsContext() throws MalformedURLException
    {
        new HttpListenerBuilder(mockMuleContext).setTlsContextFactory(mockTlsContextFactory).setListenerConfig(mockListenerConfig);
    }

    @Test
    public void useExistentListenerConfig() throws Exception
    {
        when((Object) (mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class))).thenReturn(mockMessageProcessingManager);
        when(mockMuleContext.getRegistry().lookupObjects(HttpListenerConfig.class)).thenReturn(Arrays.<HttpListenerConfig>asList(mockListenerConfig));
        when((Object) (mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class))).thenReturn(mockListenerConnectionManager);
        when(mockMuleContext.getRegistry().get(anyString())).thenReturn(null);
        when(mockListenerConfig.getPort()).thenReturn(PORT);
        when(mockListenerConfig.getHost()).thenReturn(HOST);

        final HttpListener httpListener = new HttpListenerBuilder(mockMuleContext)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(PORT)
                .setPath(PATH).build();

        assertThat(httpListener.getConfig(), Is.<HttpListenerConfig>is(mockListenerConfig));
    }

    @Test
    public void createListenerConfigIfThereIsNoMatch() throws Exception
    {
        new HttpListenerBuilder(createMuleContext())
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(PORT)
                .setPath(PATH).build();

        verify(mockListenerConnectionManager).createServer(eq(new ServerAddress(IP, PORT)), any(WorkManagerSource.class), eq(true), eq(DefaultHttpListenerConfig.DEFAULT_CONNECTION_IDLE_TIMEOUT));
    }

    @Test
    public void createListenerSslConfigIfThereIsNoMatch() throws Exception
    {
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);

        new HttpListenerBuilder(createMuleContext())
                .setTlsContextFactory(mockTlsContextFactory)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(PORT)
                .setPath(PATH).build();

        verify(mockListenerConnectionManager).createSslServer(eq(new ServerAddress(IP, PORT)), any(WorkManagerSource.class), eq(mockTlsContextFactory), eq(true), eq(DefaultHttpListenerConfig.DEFAULT_CONNECTION_IDLE_TIMEOUT));
    }

    @Test
    public void useConfiguredListenerConfig() throws Exception
    {
        when((Object) (mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class))).thenReturn(mockMessageProcessingManager);

        final HttpListener httpListener = new HttpListenerBuilder(mockMuleContext)
                .setFlow(mockFlow)
                .setListenerConfig(mockListenerConfig)
                .setPath(PATH).build();

        assertThat(httpListener.getConfig(), Is.<HttpListenerConfig>is(mockListenerConfig));
    }

    private MuleContext createMuleContext() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        muleContext.getRegistry().registerObject(HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER, mockListenerConnectionManager);

        return muleContext;
    }
}
