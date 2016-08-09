/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequesterConfig.OBJECT_HTTP_CLIENT_FACTORY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.runtime.api.tls.TlsContextFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultHttpRequesterConfigTestCase extends AbstractMuleTestCase {

  private static final String LOCALHOST = "localhost";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private TlsContextFactory mockTlsContextFactory = mock(TlsContextFactory.class);
  private DefaultHttpRequesterConfig requestConfig;

  @Before
  public void setUp() throws RegistrationException {
    when(mockTlsContextFactory.isKeyStoreConfigured()).thenReturn(true);
    requestConfig = createBaseRequester();

    MuleRegistry registry = mock(MuleRegistry.class);
    when(registry.get(OBJECT_HTTP_CLIENT_FACTORY)).thenReturn(null);
    when(mockMuleContext.getRegistry()).thenReturn(registry);
  }

  @Test
  public void defaultPortWithDefaultProtocol() throws Exception {
    requestConfig.initialise();
    assertThat(requestConfig.getPort(), is(String.valueOf(HTTP.getDefaultPort())));
  }

  @Test
  public void defaultPortWithHttpConfigured() throws Exception {
    requestConfig.setProtocol(HTTP);
    requestConfig.initialise();
    assertThat(requestConfig.getPort(), is(String.valueOf(HTTP.getDefaultPort())));
  }

  @Test
  public void defaultPortWithHttpsConfigured() throws Exception {
    requestConfig.setProtocol(HTTPS);
    requestConfig.setTlsContext(mockTlsContextFactory);
    requestConfig.initialise();
    assertThat(requestConfig.getPort(), is(String.valueOf(HTTPS.getDefaultPort())));
  }

  private DefaultHttpRequesterConfig createBaseRequester() {
    DefaultHttpRequesterConfig requestConfig = new DefaultHttpRequesterConfig();
    requestConfig.setHost(LOCALHOST);
    requestConfig.setMuleContext(mockMuleContext);
    return requestConfig;
  }
}
