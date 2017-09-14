/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SourceConnectionProviderTestCase extends AbstractMuleTestCase {

  @Mock
  private ConnectionProvider connectionProvider;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Mock
  private Object configurationObject;

  @Mock
  private Object connection;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private SourceConnectionProvider sourceConnectionProvider;
  private ConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    when(configurationInstance.getValue()).thenReturn(configurationObject);
    connectionManager = new DefaultConnectionManager(muleContext);
    connectionManager.bind(configurationObject, connectionProvider);

    sourceConnectionProvider =
        new SourceConnectionProvider(new SourceConnectionManager(connectionManager), configurationInstance);
    when(connectionProvider.connect()).thenReturn(connection);
  }

  @Test
  public void testConnection() throws Exception {
    Object testeableConnection = sourceConnectionProvider.connect();
    assertThat(testeableConnection, is(sameInstance(testeableConnection)));

    verify(connectionProvider, never()).validate(testeableConnection);

    sourceConnectionProvider.validate(testeableConnection);
    verify(connectionProvider).validate(testeableConnection);
  }
}
