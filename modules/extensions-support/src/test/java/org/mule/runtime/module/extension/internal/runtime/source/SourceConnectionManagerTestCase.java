/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class SourceConnectionManagerTestCase extends AbstractMuleTestCase {

  @Mock
  private ConnectionProvider connectionProvider;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Mock
  private Object configurationObject;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private ConnectionManager connectionManager;
  private SourceConnectionManager sourceConnectionManager;
  private Object connectionA;

  @BeforeEach
  public void before() throws Exception {
    when(connectionProvider.connect()).thenReturn(new Object());

    when(configurationInstance.getValue()).thenReturn(configurationObject);
    connectionManager = new DefaultConnectionManager(muleContext);
    initialiseIfNeeded(connectionManager);
    connectionManager.bind(configurationObject, connectionProvider);
    sourceConnectionManager = new SourceConnectionManager(connectionManager);

    sourceConnectionManager.getConnection(configurationInstance);
    connectionA = sourceConnectionManager.getConnection(configurationInstance);
  }

  @Test
  void reusedConnectionOnReleaseNotDeletedUntilAllReferencesLost() throws ConnectionException {
    sourceConnectionManager.release(connectionA);

    assertThat(sourceConnectionManager.getConnectionHandler(connectionA).isPresent(), is(true));

    sourceConnectionManager.release(connectionA);

    assertThat(sourceConnectionManager.getConnectionHandler(connectionA).isPresent(), is(false));

    // This must be ignored
    sourceConnectionManager.release(connectionA);
  }

  @Test
  void reusedConnectionOnInvalidateNotDeletedUntilAllReferencesLost() throws ConnectionException {
    sourceConnectionManager.invalidate(connectionA);

    assertThat(sourceConnectionManager.getConnectionHandler(connectionA).isPresent(), is(true));

    sourceConnectionManager.invalidate(connectionA);

    assertThat(sourceConnectionManager.getConnectionHandler(connectionA).isPresent(), is(false));

    // This must be ignored
    sourceConnectionManager.invalidate(connectionA);
  }
}
