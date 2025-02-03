/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.oauth.client.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UnauthorizeOperationExecutorTest {

  private UnauthorizeOperationExecutor executor;

  @Mock
  private ExecutionContextAdapter<ComponentModel> executionContext;

  @Mock
  private ExecutorCallback callback;

  @Mock
  private OAuthConnectionProviderWrapper provider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    ConfigurationInstance mockConfigurationInstance = mock(ConfigurationInstance.class);
    when(executionContext.getConfiguration()).thenReturn(Optional.of(mockConfigurationInstance));
    when(mockConfigurationInstance.getConnectionProvider()).thenReturn(Optional.of(provider));
    executor = new UnauthorizeOperationExecutor();
  }

  @Test
  public void testExecute_WithResourceOwnerId() {
    when(executionContext.hasParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)).thenReturn(true);
    when(executionContext.getParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)).thenReturn("testOwnerId");

    executor.execute(executionContext, callback);

    verify(provider).invalidate("testOwnerId");
    verify(callback).complete(null);
  }

  @Test
  public void testExecute_WithoutResourceOwnerId() {
    when(executionContext.hasParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)).thenReturn(false);

    executor.execute(executionContext, callback);

    verify(provider).invalidate(DEFAULT_RESOURCE_OWNER_ID);
    verify(callback).complete(null);
  }
}
