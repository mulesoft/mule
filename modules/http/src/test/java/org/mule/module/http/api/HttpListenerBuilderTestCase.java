/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.construct.Flow;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerBuilder;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.http.internal.listener.HttpListenerConnectionManager;
import org.mule.module.http.internal.listener.ServerAddress;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ssl.api.TlsContextFactory;

import java.net.MalformedURLException;
import java.net.URL;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerBuilderTestCase extends FunctionalTestCase
{

    public static URL TEST_URL;
    public static final String PATH = "somePath";

    public static final String HOST = "localhost";
    public static final String IP = "127.0.0.1";

    @Rule
    public final DynamicPort port = new DynamicPort("port");

    private TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class, RETURNS_DEEP_STUBS);
    private DefaultHttpListenerConfig mockListenerConfig = mock(DefaultHttpListenerConfig.class);
    private Flow mockFlow = mock(Flow.class);

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

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetPortAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setUrl(TEST_URL).setPort(port.getNumber());
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetHostAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setUrl(TEST_URL).setHost(HOST);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetPathAfterSetUrl() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setUrl(TEST_URL).setPath(PATH);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrlAfterSetHost() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setPort(port.getNumber()).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrltAfterSetPort() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setHost(HOST).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetUrlAfterSetPath() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setPath(PATH).setUrl(TEST_URL);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetTlsContextIfProtocolIsHttp() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setUrl(TEST_URL).setTlsContextFactory(mockTlsContextFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetTlsContextIfThereIsAListenerConfig() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setListenerConfig(mockListenerConfig).setTlsContextFactory(mockTlsContextFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void doNotAllowSetAListenerConfigIfThereIsATlsContext() throws MalformedURLException
    {
        new HttpListenerBuilder(muleContext).setTlsContextFactory(mockTlsContextFactory).setListenerConfig(mockListenerConfig);
    }

    @Test
    public void useExistentListenerConfig() throws Exception
    {
        when(mockListenerConfig.getPort()).thenReturn(port.getNumber());
        when(mockListenerConfig.getHost()).thenReturn(HOST);
        when(mockListenerConfig.resolvePath(anyString())).thenCallRealMethod();

        final HttpListener httpListener = new HttpListenerBuilder(muleContext)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(port.getNumber())
                .setListenerConfig(mockListenerConfig)
                .setPath(PATH).build();

        assertThat(httpListener.getConfig(), Is.<HttpListenerConfig>is(mockListenerConfig));
    }

    @Test
    public void createListenerConfigIfThereIsNoMatch() throws Exception
    {
        new HttpListenerBuilder(muleContext)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(port.getNumber())
                .setPath(PATH).build();

        assertServerCreated();
    }

    @Test
    public void createListenerSslConfigIfThereIsNoMatch() throws Exception
    {
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);

        new HttpListenerBuilder(muleContext)
                .setTlsContextFactory(mockTlsContextFactory)
                .setFlow(mockFlow)
                .setHost(HOST)
                .setPort(port.getNumber())
                .setPath(PATH).build();

        assertServerCreated();
    }

    @Test
    public void useConfiguredListenerConfig() throws Exception
    {
        when(mockListenerConfig.resolvePath(anyString())).thenCallRealMethod();

        final HttpListener httpListener = new HttpListenerBuilder(muleContext)
                .setFlow(mockFlow)
                .setListenerConfig(mockListenerConfig)
                .setPath(PATH).build();

        assertThat(httpListener.getConfig(), Is.<HttpListenerConfig>is(mockListenerConfig));
    }

    protected void assertServerCreated() throws org.mule.api.registry.RegistrationException
    {
        muleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class).containsServerFor(new ServerAddress(IP, port.getNumber()));
    }
}
