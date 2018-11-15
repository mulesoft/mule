/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.tck.util.MuleContextUtils;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionConnectivityTestingStrategyTestCase extends AbstractMuleTestCase {

  private ExtensionConnectivityTestingStrategy connectivityTestingStrategy;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConnectionProviderResolver connectionProviderResolver;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConnectionProvider connectionProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtendedExpressionManager expressionManager;

  private ConnectionManager connectionManager = new DefaultConnectionManager(muleContext);

  @Before
  public void createTestingInstance() throws MuleException {
    connectivityTestingStrategy = new ExtensionConnectivityTestingStrategy(connectionManager, muleContext);
    MuleContextUtils.mockContextWithServices().getInjector().inject(connectivityTestingStrategy);
  }

  @Test
  public void connectionProviderInConfigWithInvalidConnection() throws MuleException {
    ConnectionValidationResult connectionResult = testConnectivityWithConnectionProvider(false);
    assertThat(connectionResult.isValid(), is(false));
  }

  @Test
  public void connectionProviderInConfigWithValidConnection() throws MuleException {
    ConnectionValidationResult connectionResult = testConnectivityWithConnectionProvider(true);
    assertThat(connectionResult.isValid(), is(true));
  }

  @Test
  public void connectionProviderThrowsException() throws MuleException {
    final Exception e = new RuntimeException();
    when(connectionProviderResolver.resolve(any())).thenThrow(e);
    ConnectionValidationResult connectionResult = connectivityTestingStrategy.testConnectivity(connectionProviderResolver);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), is(sameInstance(e)));
  }

  private ConnectionValidationResult testConnectivityWithConnectionProvider(boolean isValidConnection) throws MuleException {
    when(connectionProviderResolver.resolve(any())).thenReturn(new Pair<>(connectionProvider, mock(ResolverSetResult.class)));
    ConnectionValidationResult validationResult;
    if (isValidConnection) {
      validationResult = ConnectionValidationResult.success();
    } else {
      validationResult = ConnectionValidationResult.failure("", null);
    }
    when(connectionProvider.validate(any())).thenReturn(validationResult);

    ConnectionValidationResult connectionResult =
        connectivityTestingStrategy.testConnectivity(connectionProviderResolver);
    return connectionResult;
  }
}
