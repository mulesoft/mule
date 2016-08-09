/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.http.api.HttpListenerConnectionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.api.tls.TlsContextFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class HttpListenerConfigTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void initializationFailsWhenNoTlsKeyStoreConfigured() throws Exception {
    final DefaultHttpListenerConfig httpListenerConfig = new DefaultHttpListenerConfig();
    httpListenerConfig.setProtocol(HTTPS);
    final MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
    httpListenerConfig.setMuleContext(mockMuleContext);
    final TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
    httpListenerConfig.setTlsContext(mockTlsContextFactory);
    when((Object) (mockMuleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class)))
        .thenReturn(mock(HttpListenerConnectionManager.class));
    when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(false);
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("KeyStore must be configured for server side SSL"));
    httpListenerConfig.initialise();
  }

}
