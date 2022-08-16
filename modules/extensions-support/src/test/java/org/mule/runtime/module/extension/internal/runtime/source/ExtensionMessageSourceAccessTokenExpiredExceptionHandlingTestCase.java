/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.util.MuleSystemProperties.SHOULD_HANDLE_ACCESS_TOKEN_EXPIRED_EXCEPTIONS_ON_SOURCES_PROPERTY;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;

@RunWith(Parameterized.class)
public class ExtensionMessageSourceAccessTokenExpiredExceptionHandlingTestCase extends AbstractExtensionMessageSourceTestCase {

  protected static final int TEST_TIMEOUT = 3000;
  protected static final int TEST_POLL_DELAY = 1000;

  @Parameterized.Parameters
  public static List<Object[]> data() {
    return asList(new Object[][] {{"true"}, {"false"}});
  }

  @Rule
  public SystemProperty shouldHandleAccessTokenExpiredException;

  public ExtensionMessageSourceAccessTokenExpiredExceptionHandlingTestCase(String shouldHandleAccessTokenExpiredException) {
    this.shouldHandleAccessTokenExpiredException = new SystemProperty(SHOULD_HANDLE_ACCESS_TOKEN_EXPIRED_EXCEPTIONS_ON_SOURCES_PROPERTY, shouldHandleAccessTokenExpiredException);
  }

  @Test
  public void initialise() throws Exception {
    if (!messageSource.getLifecycleState().isInitialised()) {
      messageSource.initialise();
      verify(muleContext.getInjector()).inject(source);
      verify((Initialisable) source).initialise();
      verify(source, never()).onStart(sourceCallback);
    }
  }

  @Test
  public void start() throws Exception {
    initialise();
    if (!messageSource.getLifecycleState().isStarted()) {
      messageSource.start();
    }

    final Injector injector = muleContext.getInjector();
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      InOrder inOrder = inOrder(injector, source);
      inOrder.verify(injector).inject(source);
      inOrder.verify((Initialisable) source).initialise();
      inOrder.verify(source).onStart(sourceCallback);
      return true;
    }));
  }

  private void mockConnectionProvider() {
    OAuthConnectionProviderWrapper connectionProvider = mock(OAuthConnectionProviderWrapper.class);
    when(messageSource.getConfigurationInstance().get().getConnectionProvider()).thenReturn(Optional.of(connectionProvider));
    OAuthGrantType grantType = mock(OAuthGrantType.class);
    when(connectionProvider.getGrantType()).thenReturn(grantType);
  }

  private AutoCloseable withExceptionListener(SystemExceptionHandler listener) {
    SystemExceptionHandler previousListener = muleContext.getExceptionListener();
    muleContext.setExceptionListener(listener);
    return () -> muleContext.setExceptionListener(previousListener);
  }

  @Test
  public void handleConnectionException() throws Exception {
    start();
    mockConnectionProvider();

    SystemExceptionHandler mockListener = mock(SystemExceptionHandler.class);
    try(AutoCloseable ignore = withExceptionListener(mockListener)) {
      ConnectionException e = new ConnectionException(ERROR_MESSAGE);
      messageSource.onException(e);

      verify(mockListener).handleException(e, messageSource.getLocation());
    }
  }

  @Test
  public void handleTokenAccessExpiredException() throws Exception {
    start();
    mockConnectionProvider();

    SystemExceptionHandler mockListener = mock(SystemExceptionHandler.class);
    try(AutoCloseable ignore = withExceptionListener(mockListener)) {
      AccessTokenExpiredException innerException = new AccessTokenExpiredException();
      ConnectionException e = new ConnectionException(innerException);
      messageSource.onException(e);

      boolean shouldHandleException = parseBoolean(shouldHandleAccessTokenExpiredException.getValue());
      if(shouldHandleException) {
        verify(mockListener).handleException(e, messageSource.getLocation());
      } else {
        verify(mockListener).handleException(e, messageSource.getLocation());
      }
    }
  }
}
