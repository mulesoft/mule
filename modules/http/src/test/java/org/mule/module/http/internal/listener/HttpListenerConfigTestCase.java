/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.internal.listener.HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.ssl.TlsContextFactory;

import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class HttpListenerConfigTestCase extends AbstractMuleTestCase
{

    @Test(expected = InitialisationException.class)
    public void initializationFailsWhenNoTlsKeyStoreConfigured() throws Exception
    {
        final DefaultHttpListenerConfig httpListenerConfig = new DefaultHttpListenerConfig();
        final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
        httpListenerConfig.setMuleContext(mockMuleContext);
        final TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
        httpListenerConfig.setTlsContext(mockTlsContextFactory);
        when(mockMuleContext.getRegistry().get(HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mock(HttpListenerConnectionManager.class));
        when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(false);
        httpListenerConfig.initialise();
    }

}
