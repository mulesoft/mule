/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConnectionArgumentResolverTestCase extends AbstractMuleTestCase {

  @Mock
  private OperationContextAdapter operationContext;

  private ConnectionArgumentResolver resolver = new ConnectionArgumentResolver();

  @Test
  public void resolve() throws Exception {
    ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    final Object connection = new Object();
    when(connectionHandler.getConnection()).thenReturn(connection);
    when(operationContext.getVariable(CONNECTION_PARAM)).thenReturn(connectionHandler);

    assertThat(resolver.resolve(operationContext), is(sameInstance(connection)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noConnection() {
    resolver.resolve(operationContext);
  }
}
