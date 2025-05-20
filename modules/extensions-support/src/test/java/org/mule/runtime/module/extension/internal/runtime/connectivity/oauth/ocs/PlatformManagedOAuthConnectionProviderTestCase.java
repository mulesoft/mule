/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class PlatformManagedOAuthConnectionProviderTestCase extends AbstractMuleTestCase {

  @Mock
  private PlatformManagedOAuthConfig mockOAuthConfig;

  @Mock
  private PlatformManagedOAuthHandler mockOAuthHandler;

  @Mock
  private ReconnectionConfig mockReconnectionConfig;

  @Mock
  private PoolingProfile mockPoolingProfile;

  @Mock
  private ConnectionValidationResult mockValidationResult;

  @Mock
  private Object mockConnection;

  private PlatformManagedOAuthConnectionProvider<Object> provider;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    ConnectionProviderModel mockProviderModel = mock(ConnectionProviderModel.class);
    when(mockOAuthConfig.getDelegateConnectionProviderModel()).thenReturn(mockProviderModel);
    provider = new PlatformManagedOAuthConnectionProvider<>(
                                                            mockOAuthConfig, mockOAuthHandler, mockReconnectionConfig,
                                                            mockPoolingProfile);
  }

  @Test
  void testGetGrantType() {
    PlatformManagedOAuthGrantType mockGrantType = mock(PlatformManagedOAuthGrantType.class);
    when(mockOAuthConfig.getGrantType()).thenReturn(mockGrantType);
    assertThat(provider.getGrantType(), is(mockGrantType));
  }

  @Test
  void testInvalidateCallsHandler() {
    provider.invalidate("testResourceOwner");
    verify(mockOAuthHandler).invalidate(mockOAuthConfig);
  }

  @Test
  void testRefreshTokenCallsHandler() {
    provider.refreshToken("testResourceOwner");
    verify(mockOAuthHandler).refreshToken(mockOAuthConfig);
  }

  @Test
  void testGetPoolingProfile() {
    assertThat(provider.getPoolingProfile().isPresent(), is(true));
    assertThat(provider.getPoolingProfile().get(), is(mockPoolingProfile));
  }

  @Test
  void testGetReconnectionConfig() {
    assertThat(provider.getReconnectionConfig().isPresent(), is(true));
    assertThat(provider.getReconnectionConfig().get(), is(mockReconnectionConfig));
  }

  @Test
  void testConnect() throws ConnectionException {
    ConnectionProvider<Object> mockDelegate = mock(ConnectionProvider.class);
    when(mockDelegate.connect()).thenReturn(mockConnection);
    when(mockOAuthConfig.getDelegateGrantType()).thenReturn(mock(OAuthGrantType.class));
    when(mockOAuthHandler.getOAuthContext(mockOAuthConfig)).thenReturn(mock(ResourceOwnerOAuthContext.class));
    provider = spy(provider);
    doReturn(mockDelegate).when(provider).getDelegate();

    Object connection = provider.connect();

    assertThat(connection, is(mockConnection));
    verify(provider).getDelegate();
    verify(mockDelegate).connect();
  }

  @Test
  void testValidate() {
    ConnectionProvider<Object> mockDelegate = mock(ConnectionProvider.class);
    ResourceOwnerOAuthContext mockContext = mock(ResourceOwnerOAuthContext.class);
    when(mockContext.getAccessToken()).thenReturn("accessToken");
    when(mockOAuthHandler.getOAuthContext(mockOAuthConfig)).thenReturn(mockContext);
    when(mockDelegate.validate(mockConnection)).thenReturn(mockValidationResult);
    provider = spy(provider);
    doReturn(mockDelegate).when(provider).getDelegate();

    ConnectionValidationResult result = provider.validate(mockConnection);

    assertThat(result, is(mockValidationResult));
    verify(provider).getDelegate();
    verify(mockDelegate).validate(mockConnection);
  }

  @Test
  void testGetOwnerConfigName() {
    String ownerConfigName = "testOwnerConfig";
    when(mockOAuthConfig.getOwnerConfigName()).thenReturn(ownerConfigName);

    Optional<String> result = provider.getOwnerConfigName();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(ownerConfigName));
  }

  @Test
  void testGetResourceOwnerId() {
    ResourceOwnerOAuthContext mockContext = mock(ResourceOwnerOAuthContext.class);
    String resourceOwnerId = "testResourceOwnerId";
    when(mockContext.getResourceOwnerId()).thenReturn(resourceOwnerId);
    when(mockOAuthHandler.getOAuthContext(mockOAuthConfig)).thenReturn(mockContext);

    String result = provider.getResourceOwnerId();

    assertThat(result, is(resourceOwnerId));
    verify(mockOAuthHandler).getOAuthContext(mockOAuthConfig);
  }

  @Test
  void testGetRetryPolicyTemplate() {
    assertThat(provider.getRetryPolicyTemplate(), is(notNullValue()));
  }

  @Test
  void testGetDelegate() {
    assertThrows(NullPointerException.class, () -> provider.getDelegate());
  }

  @Test
  void testOnBorrow() throws MuleException {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    ConnectionProvider<Object> mockConnectionProvider = mock(ConnectionProvider.class);
    PoolingListener<Object> listener = mock(PoolingListener.class);
    doReturn(mockConnectionProvider).when(spyProvider).createDelegate(any());
    doNothing().when(spyProvider).initialiseDelegate();
    doReturn(listener).when(spyProvider).getDelegatePoolingListener();

    PlatformManagedOAuthDancer mockDancer = mock(PlatformManagedOAuthDancer.class);
    when(mockOAuthHandler.register(mockOAuthConfig)).thenReturn(mockDancer);

    PlatformManagedConnectionDescriptor mockPlatformManagedConnectionDescriptor = mock(PlatformManagedConnectionDescriptor.class);
    CompletableFuture<PlatformManagedConnectionDescriptor> future =
        CompletableFuture.completedFuture(mockPlatformManagedConnectionDescriptor);
    when(mockDancer.getConnectionDescriptor()).thenReturn(future);

    spyProvider.start();

    doNothing().when(listener).onBorrow(mockConnection);
    spyProvider.onBorrow(mockConnection);
    verify(listener, times(1)).onBorrow(mockConnection);
  }

  @Test
  void testOnReturn() throws MuleException {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    ConnectionProvider<Object> mockConnectionProvider = mock(ConnectionProvider.class);
    PoolingListener<Object> listener = mock(PoolingListener.class);
    doReturn(mockConnectionProvider).when(spyProvider).createDelegate(any());
    doNothing().when(spyProvider).initialiseDelegate();
    doReturn(listener).when(spyProvider).getDelegatePoolingListener();

    PlatformManagedOAuthDancer mockDancer = mock(PlatformManagedOAuthDancer.class);
    when(mockOAuthHandler.register(mockOAuthConfig)).thenReturn(mockDancer);

    PlatformManagedConnectionDescriptor mockPlatformManagedConnectionDescriptor = mock(PlatformManagedConnectionDescriptor.class);
    CompletableFuture<PlatformManagedConnectionDescriptor> future =
        CompletableFuture.completedFuture(mockPlatformManagedConnectionDescriptor);
    when(mockDancer.getConnectionDescriptor()).thenReturn(future);

    spyProvider.start();

    doNothing().when(listener).onBorrow(mockConnection);
    spyProvider.onReturn(mockConnection);
    verify(listener, times(1)).onReturn(mockConnection);
  }

  @Test
  void testStartFailure() throws MuleException, ExecutionException, InterruptedException {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    ConnectionProvider<Object> mockConnectionProvider = mock(ConnectionProvider.class);
    PoolingListener<Object> listener = mock(PoolingListener.class);
    doReturn(mockConnectionProvider).when(spyProvider).createDelegate(any());
    doNothing().when(spyProvider).initialiseDelegate();
    doReturn(listener).when(spyProvider).getDelegatePoolingListener();

    PlatformManagedOAuthDancer mockDancer = mock(PlatformManagedOAuthDancer.class);
    when(mockOAuthHandler.register(mockOAuthConfig)).thenReturn(mockDancer);

    CompletableFuture<PlatformManagedConnectionDescriptor> future = mock(CompletableFuture.class);
    when(mockDancer.getConnectionDescriptor()).thenReturn(future);
    doThrow(new ExecutionException("Token refresh failed", new Throwable()))
        .when(future).get();

    assertThrows(MuleException.class, () -> spyProvider.start());
  }

  @Test
  void testStop() throws MuleException {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    spyProvider.stop();
  }

  @Test
  void testDispose() throws MuleException {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    spyProvider.dispose();
  }

  @Test
  void testDisconnect() {
    PlatformManagedOAuthConnectionProvider<Object> spyProvider = spy(provider);
    ConnectionProvider<Object> mockDelegate = mock(ConnectionProvider.class);
    doNothing().when(mockDelegate).disconnect(mockConnection);

    doReturn(mockDelegate).when(spyProvider).getDelegate();
    spyProvider.disconnect(mockConnection);
    verify(mockDelegate, times(1)).disconnect(mockConnection);
  }

  @Test
  void testGetConnectionManagementType() {
    ConnectionProviderModel mockModel = mock(ConnectionProviderModel.class);
    ConnectionManagementType mockType = mock(ConnectionManagementType.class);
    when(mockOAuthConfig.getDelegateConnectionProviderModel()).thenReturn(mockModel);
    when(mockModel.getConnectionManagementType()).thenReturn(mockType);
    ConnectionManagementType managementType = provider.getConnectionManagementType();

    assertThat(managementType, is(mockType));
  }

  @Test
  void supportsXa() {
    // TODO W-18557890 update this with the mock extModel
    boolean supportsXa = provider.supportsXa();

    assertThat(supportsXa, is(false));
  }
}
