/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.service.http.api.server.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.junit.Before;
import org.junit.Test;

public abstract class BaseResponseCompletionHandlerTestCase extends AbstractMuleTestCase {

  private static final String ERROR = "Error";
  protected FilterChainContext ctx = mock(FilterChainContext.class);
  protected Connection connection = mock(Connection.class, RETURNS_DEEP_STUBS);
  protected HttpRequestPacket request = mock(HttpRequestPacket.class);
  protected ResponseStatusCallback callback = spy(ResponseStatusCallback.class);

  protected abstract BaseResponseCompletionHandler getHandler();

  @Before
  public void setUp() {
    when(ctx.getConnection()).thenReturn(connection);
  }

  @Test
  public void failedTaskAvoidsResponse() {
    when(connection.isOpen()).thenReturn(false);
    getHandler().failed(new IOException(ERROR));
    verify(callback, never()).responseSendFailure(any(Throwable.class));
  }

  @Test
  public void cancelledTaskResponse() {
    when(connection.isOpen()).thenReturn(true);
    getHandler().cancelled();
    verify(callback, atLeastOnce()).responseSendFailure(any(Throwable.class));
  }



}
