/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.api.TlsContextFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultHttpListenerConfigTestCase extends AbstractMuleTestCase
{

    private static final String LOCALHOST = "localhost";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private HttpListenerConnectionManager mockHttpListenerConnectionManager = mock(HttpListenerConnectionManager.class);
    private TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
    private DefaultHttpListenerConfig listenerConfig;

    @Before
    public void setUp() throws RegistrationException
    {
        when((Object) (mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class))).thenReturn(mockHttpListenerConnectionManager);
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);
        listenerConfig = createBaseListener();
    }

    @Test
    public void defaultPortWithDefaultProtocol() throws Exception
    {
        listenerConfig.initialise();
        assertThat(listenerConfig.getPort(), is(HTTP.getDefaultPort()));
    }

    @Test
    public void defaultPortWithHttpConfigured() throws Exception
    {
        listenerConfig.setProtocol(HTTP);
        listenerConfig.initialise();
        assertThat(listenerConfig.getPort(), is(HTTP.getDefaultPort()));
    }

    @Test
    public void defaultPortWithHttpsConfigured() throws Exception
    {
        listenerConfig.setProtocol(HTTPS);
        listenerConfig.setTlsContext(mockTlsContextFactory);
        listenerConfig.initialise();
        assertThat(listenerConfig.getPort(), is(HTTPS.getDefaultPort()));
    }

    @Test
    public void validateTlsContextWhenUsingHttps() throws Exception
    {
        listenerConfig.setProtocol(HTTPS);
        expectedException.expect(InitialisationException.class);
        listenerConfig.initialise();
    }

    @Test
    public void validateTlsContextWithNoKeystoreWhenUsingHttps() throws Exception
    {
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(false);
        listenerConfig.setProtocol(HTTPS);
        expectedException.expect(InitialisationException.class);
        listenerConfig.initialise();
    }

    private DefaultHttpListenerConfig createBaseListener()
    {
        DefaultHttpListenerConfig listenerConfig = new DefaultHttpListenerConfig(mockHttpListenerConnectionManager);
        listenerConfig.setHost(LOCALHOST);
        listenerConfig.setMuleContext(mockMuleContext);
        return listenerConfig;
    }
}