/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequesterConfig.OBJECT_HTTP_CLIENT_FACTORY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class HttpRequestConnectionsConfigurationTestCase extends AbstractMuleTestCase {

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());

  @Before
  public void before() {
    MuleRegistry registry = mock(MuleRegistry.class);
    when(registry.get(OBJECT_HTTP_CLIENT_FACTORY)).thenReturn(null);
    when(mockMuleContext.getRegistry()).thenReturn(registry);
  }

  @Test(expected = InitialisationException.class)
  public void invalidMaxConnections() throws InitialisationException {
    DefaultHttpRequesterConfig httpRequesterConfig = createRequesterConfig();
    httpRequesterConfig.setMaxConnections(-2);
    httpRequesterConfig.initialise();
  }

  @Test(expected = InitialisationException.class)
  public void invalidMaxConnections0() throws InitialisationException {
    DefaultHttpRequesterConfig httpRequesterConfig = createRequesterConfig();
    httpRequesterConfig.setMaxConnections(0);
    httpRequesterConfig.initialise();
  }

  @Test
  public void ignoreIdleTimeoutIfNotPersistentConnections() throws MuleException {
    DefaultHttpRequesterConfig httpRequesterConfig = createRequesterConfig();
    httpRequesterConfig.setUsePersistentConnections(false);
    httpRequesterConfig.setConnectionIdleTimeout(-2);
    httpRequesterConfig.initialise();
    httpRequesterConfig.stop();
  }

  private DefaultHttpRequesterConfig createRequesterConfig() {
    DefaultHttpRequesterConfig requesterConfig = new DefaultHttpRequesterConfig();
    requesterConfig.setMuleContext(mockMuleContext);
    requesterConfig.setName("TestConfig");
    return requesterConfig;
  }
}
