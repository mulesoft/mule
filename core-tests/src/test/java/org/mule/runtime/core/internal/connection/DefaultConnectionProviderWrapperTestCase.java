/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.Injector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class DefaultConnectionProviderWrapperTestCase extends AbstractMuleTestCase {

  private static final String ERROR_MESSAGE = "BOOM ><";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private ConnectionProvider<Object> connectionProvider;

  @Mock
  private Object connection;

  private DefaultConnectionProviderWrapper<Object> wrapper;

  @Mock
  private Injector injector;

  @Before
  public void before() throws Exception {
    when(connectionProvider.connect()).thenReturn(connection);
    wrapper = new DefaultConnectionProviderWrapper<>(connectionProvider, injector);
  }

  @Test
  public void connect() throws Exception {
    wrapper.connect();
    verify(connectionProvider).connect();
    verify(injector).inject(connection);
  }


  @Test
  public void disconnect() throws Exception {
    wrapper.disconnect(connection);
    verify(connectionProvider).disconnect(connection);
  }

  @Test
  public void alwaysThrowConnectionException() throws ConnectionException {
    wrapper = new DefaultConnectionProviderWrapper(new TestProvider(), injector);
    var thrown = assertThrows(ConnectionException.class, () -> wrapper.connect());
    assertThat(thrown.getCause(), instanceOf(NullPointerException.class));
    assertThat(thrown.getMessage(), containsString(ERROR_MESSAGE));
  }

  private class TestProvider implements ConnectionProvider {

    @Override
    public Object connect() throws ConnectionException {
      throw new NullPointerException(ERROR_MESSAGE);
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return success();
    }
  }
}
