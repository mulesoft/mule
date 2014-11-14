/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.construct.Flow;
import org.mule.execution.MessageProcessingManager;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerBuilder;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.http.internal.listener.HttpListenerConnectionManager;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.TlsContextFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class HttpListenerBuilderTestCase
{

    public static URL TEST_URL;
    public static final String PATH = "somePath";
    public static final int PORT = 1000;
    public static final String HOST = "localhost";

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
        when(mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class)).thenReturn(mockMessageProcessingManager);
        when(mockMuleContext.getRegistry().lookupObjects(HttpListenerConfig.class)).thenReturn(Arrays.<HttpListenerConfig>asList(mockListenerConfig));
        when(mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)).thenReturn(mockListenerConnectionManager);
        when(mockMuleContext.getRegistry().get(anyString())).thenReturn(null);
        when(mockListenerConfig.getPort()).thenReturn(PORT);
        when(mockListenerConfig.getHost()).thenReturn(HOST);
        when(mockListenerConfig.resolvePath(anyString())).thenCallRealMethod();

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
        when(mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class)).thenReturn(mockMessageProcessingManager);
        when(mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)).thenReturn(mockListenerConnectionManager);
        when(mockMuleContext.getRegistry().lookupObjects(HttpListenerConfig.class)).thenReturn(Collections.<HttpListenerConfig>emptyList());
        when(mockMuleContext.getRegistry().get(anyString())).thenReturn(null);

        new HttpListenerBuilder(mockMuleContext)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(PORT)
                .setPath(PATH).build();

        verify(mockListenerConnectionManager).createServer(new ServerAddress(HOST, PORT), true, DefaultHttpListenerConfig.DEFAULT_CONNECTION_IDLE_TIMEOUT);
    }

    @Test
    public void createListenerSslConfigIfThereIsNoMatch() throws Exception
    {
        when(mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class)).thenReturn(mockMessageProcessingManager);
        when(mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)).thenReturn(mockListenerConnectionManager);
        when(mockMuleContext.getRegistry().lookupObjects(DefaultHttpListenerConfig.class)).thenReturn(Collections.<DefaultHttpListenerConfig>emptyList());
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);
        when(mockMuleContext.getRegistry().get(anyString())).thenReturn(null);

        new HttpListenerBuilder(mockMuleContext)
                .setTlsContextFactory(mockTlsContextFactory)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(PORT)
                .setPath(PATH).build();

        verify(mockListenerConnectionManager).createSslServer(new ServerAddress(HOST, PORT), mockTlsContextFactory, true, DefaultHttpListenerConfig.DEFAULT_CONNECTION_IDLE_TIMEOUT);
    }

    @Test
    public void useConfiguredListenerConfig() throws Exception
    {
        when(mockMuleContext.getRegistry().lookupObject(MessageProcessingManager.class)).thenReturn(mockMessageProcessingManager);
        when(mockListenerConfig.resolvePath(anyString())).thenCallRealMethod();

        final HttpListener httpListener = new HttpListenerBuilder(mockMuleContext)
                .setFlow(mockFlow)
                .setListenerConfig(mockListenerConfig)
                .setPath(PATH).build();

        assertThat(httpListener.getConfig(), Is.<HttpListenerConfig>is(mockListenerConfig));
    }
}
