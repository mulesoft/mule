/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.Injector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultConnectionProviderWrapperTestCase extends AbstractMuleContextTestCase {

  private static final String ERROR_MESSAGE = "BOOM ><";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private ConnectionProvider<Object> connectionProvider;

  @Mock
  private Object connection;

  private DefaultConnectionProviderWrapper<Object> wrapper;

  @Mock
  private Injector injector;

  @Before
  public void before() throws Exception {
    muleContext = spy(muleContext);
    when(muleContext.getInjector()).thenReturn(injector);
    when(connectionProvider.connect()).thenReturn(connection);
    wrapper = new DefaultConnectionProviderWrapper<>(connectionProvider, muleContext);
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
    exception.expect(ConnectionException.class);
    exception.expectCause(instanceOf(NullPointerException.class));
    exception.expectMessage(ERROR_MESSAGE);
    wrapper = new DefaultConnectionProviderWrapper(new TestProvider(), muleContext);
    wrapper.connect();
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
