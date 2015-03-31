/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.HttpConstants;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.api.TlsContextFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;

@SmallTest
public class HttpListenerConfigTestCase extends AbstractMuleTestCase
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void initializationFailsWhenNoTlsKeyStoreConfigured() throws Exception
    {
        final DefaultHttpListenerConfig httpListenerConfig = new DefaultHttpListenerConfig();
        httpListenerConfig.setProtocol(HttpConstants.Protocols.HTTPS);
        final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
        httpListenerConfig.setMuleContext(mockMuleContext);
        final TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
        httpListenerConfig.setTlsContext(mockTlsContextFactory);
        when(mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)).thenReturn(mock(HttpListenerConnectionManager.class));
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(false);
        expectedException.expect(InitialisationException.class);
        expectedException.expectMessage(containsString("KeyStore must be configured for server side SSL"));
        httpListenerConfig.initialise();
    }

    @Test
    public void initializationFailsWhenTrustStoreSetButNotConfigured() throws Exception
    {
        final DefaultHttpListenerConfig httpListenerConfig = new DefaultHttpListenerConfig();
        httpListenerConfig.setProtocol(HttpConstants.Protocols.HTTPS);
        final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
        httpListenerConfig.setMuleContext(mockMuleContext);
        final TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
        httpListenerConfig.setTlsContext(mockTlsContextFactory);
        when(mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)).thenReturn(mock(HttpListenerConnectionManager.class));
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);
        when(mockTlsContextFactory.isTrustStoreValid()).thenReturn(false);
        expectedException.expect(InitialisationException.class);
        expectedException.expectMessage(containsString("Specified TrustStore could not be configured"));
        httpListenerConfig.initialise();
    }
}
