/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.tooling;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExtensionConnectivityTestingStrategyTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private ExtensionConnectivityTestingStrategy extensionConnectivityTestingStrategy;
  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
  private ConnectionProviderResolver mockConnectionProviderResolver =
      mock(ConnectionProviderResolver.class, RETURNS_DEEP_STUBS.get());
  private ConnectionValidationResult mockConnectionValidationResult =
      mock(ConnectionValidationResult.class, RETURNS_DEEP_STUBS.get());
  private ConnectionProvider mockConnectionProvider = mock(ConnectionProvider.class, RETURNS_DEEP_STUBS.get());


  @Before
  public void createTestingInstance() {
    extensionConnectivityTestingStrategy = new ExtensionConnectivityTestingStrategy();
    extensionConnectivityTestingStrategy.setMuleContext(mockMuleContext);
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
  public void connectionProviderThrowsExceprtion() throws MuleException {
    when(mockConnectionProviderResolver.resolve(any())).thenThrow(mock(RuntimeException.class));
    ConnectionValidationResult connectionResult =
        extensionConnectivityTestingStrategy.testConnectivity(mockConnectionProviderResolver);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), instanceOf(RuntimeException.class));
  }

  private ConnectionValidationResult testConnectivityWithConnectionProvider(boolean isValidConnection) throws MuleException {
    when(mockConnectionProviderResolver.resolve(any())).thenReturn(mockConnectionProvider);
    when(mockConnectionProvider.validate(any())).thenReturn(mockConnectionValidationResult);
    when(mockConnectionValidationResult.isValid()).thenReturn(isValidConnection);
    ConnectionValidationResult connectionResult =
        extensionConnectivityTestingStrategy.testConnectivity(mockConnectionProviderResolver);
    return connectionResult;
  }

}
