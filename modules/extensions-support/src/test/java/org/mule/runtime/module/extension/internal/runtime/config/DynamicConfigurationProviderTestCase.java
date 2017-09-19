/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockInterceptors;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigurationProviderTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension> {

  private static final Class MODULE_CLASS = HeisenbergExtension.class;

  @Rule
  public ExpectedException expected = none();

  @Mock
  private ResolverSet resolverSet;

  @Mock
  private OperationModel operationModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ResolverSetResult resolverSetResult;

  @Mock
  private ConnectionProviderResolver connectionProviderResolver;

  @Mock
  private ConnectionProvider connectionProvider;

  private ExpirationPolicy expirationPolicy;

  @Override
  @Before
  public void before() throws Exception {
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());
    mockInterceptors(configurationModel, null);
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getSourceModels()).thenReturn(ImmutableList.of());

    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionModel.getSourceModels()).thenReturn(ImmutableList.of());
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(operationModel.requiresConnection()).thenReturn(true);
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getSourceModels()).thenReturn(ImmutableList.of());

    when(resolverSet.resolve(from(event))).thenReturn(resolverSetResult);
    when(resolverSetResult.asMap()).thenReturn(new HashMap<>());
    visitableMock(operationModel);


    expirationPolicy = new ImmutableExpirationPolicy(5, MINUTES, timeSupplier);

    when(connectionProviderResolver.getResolverSet()).thenReturn(empty());
    when(connectionProviderResolver.resolve(any())).thenReturn(null);
    provider = new DynamicConfigurationProvider(CONFIG_NAME, extensionModel, configurationModel, resolverSet,
                                                connectionProviderResolver, expirationPolicy, muleContext);

    super.before();
    provider.initialise();
    provider.start();
  }

  @After
  public void after() throws MuleException {
    provider.stop();
    provider.dispose();
  }

  @Test
  public void resolveCached() throws Exception {
    final int count = 10;
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    for (int i = 1; i < count; i++) {
      assertThat(provider.get(event).getValue(), is(sameInstance(config)));
    }

    verify(resolverSet, times(count)).resolve(from(event));
  }

  @Test
  public void resolveCachedWithProviderParams() throws Exception {
    ResolverSet providerResolverSet = mock(ResolverSet.class);
    when(connectionProviderResolver.getResolverSet()).thenReturn(of(providerResolverSet));
    when(connectionProviderResolver.getObjectBuilder()).thenReturn(empty());
    when(providerResolverSet.resolve(from(event))).thenReturn(resolverSetResult);

    final int count = 10;
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    for (int i = 1; i < count; i++) {
      assertThat(provider.get(event).getValue(), is(sameInstance(config)));
    }

    verify(providerResolverSet, times(count)).resolve(from(event));
    verify(resolverSet, times(count)).resolve(from(event));
  }

  @Test
  public void resolveProviderParamsDifferentInstance() throws Exception {
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());

    ResolverSet providerResolverSet = mock(ResolverSet.class);
    when(connectionProviderResolver.getResolverSet()).thenReturn(of(providerResolverSet));
    when(connectionProviderResolver.getObjectBuilder()).thenReturn(empty());
    when(providerResolverSet.resolve(from(event))).thenReturn(mock(ResolverSetResult.class));
    assertThat(provider.get(event).getValue(), is(not(sameInstance(config))));

    verify(resolverSet, times(2)).resolve(from(event));
    verify(providerResolverSet, times(1)).resolve(from(event));
    verify(connectionProviderResolver, times(2)).resolve(from(event));
  }

  @Test
  public void resolveDifferentInstances() throws Exception {
    HeisenbergExtension instance1 = (HeisenbergExtension) provider.get(event).getValue();
    HeisenbergExtension instance2 = makeAlternateInstance();

    assertThat(instance2, is(not(sameInstance(instance1))));
  }

  @Test
  public void getExpired() throws Exception {
    HeisenbergExtension instance1 = (HeisenbergExtension) provider.get(event).getValue();
    HeisenbergExtension instance2 = makeAlternateInstance();

    DynamicConfigurationProvider provider = (DynamicConfigurationProvider) this.provider;
    timeSupplier.move(1, MINUTES);

    List<ConfigurationInstance> expired = provider.getExpired();
    assertThat(expired.isEmpty(), is(true));

    timeSupplier.move(10, MINUTES);

    expired = provider.getExpired();
    assertThat(expired.isEmpty(), is(false));

    List<Object> configs = expired.stream().map(config -> config.getValue()).collect(toImmutableList());
    assertThat(configs, containsInAnyOrder(instance1, instance2));
  }

  private HeisenbergExtension makeAlternateInstance() throws Exception {
    ResolverSetResult alternateResult = mock(ResolverSetResult.class, Mockito.RETURNS_DEEP_STUBS);
    when(alternateResult.asMap()).thenReturn(new HashMap<>());
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());
    when(resolverSet.resolve(from(event))).thenReturn(alternateResult);

    return (HeisenbergExtension) provider.get(event).getValue();
  }

  @Test
  public void resolveDynamicConfigWithEquivalentEvent() throws Exception {
    assertSameInstancesResolved();
  }

  @Test
  public void resolveDynamicConfigWithDifferentEvent() throws Exception {
    Object config1 = provider.get(event);

    when(resolverSet.resolve(from(event))).thenReturn(mock(ResolverSetResult.class));
    Object config2 = provider.get(event);

    assertThat(config1, is(not(sameInstance(config2))));
  }

  @Test
  public void configFailsOnInitialize() throws Exception {
    final Lifecycle connProvider = mock(Lifecycle.class, withSettings().extraInterfaces(ConnectionProvider.class));
    final String expectedExceptionMessage = "Init failed!";
    doThrow(new RuntimeException(expectedExceptionMessage)).when(connProvider).initialise();

    when(connectionProviderResolver.resolve(any())).thenReturn(new Pair<>(connProvider, mock(ResolverSetResult.class)));

    expected.expectCause(hasMessage(is(InitialisationException.class.getName() + ": " + expectedExceptionMessage)));

    try {
      provider.get(event);
    } finally {
      verify(connProvider).initialise();
      verify(connProvider, never()).start();
      verify(connProvider, never()).stop();
      verify(connProvider).dispose();
    }
  }

  @Test
  public void configFailsOnStart() throws Exception {
    final Lifecycle connProvider = mock(Lifecycle.class, withSettings().extraInterfaces(ConnectionProvider.class));
    final RuntimeException toThrow = new RuntimeException("Start failed!");
    doThrow(toThrow).when(connProvider).start();

    when(connectionProviderResolver.resolve(any())).thenReturn(new Pair<>(connProvider, resolverSetResult));

    expected.expectCause(sameInstance(toThrow));

    try {
      provider.get(event);
    } finally {
      verify(connProvider).initialise();
      verify(connProvider).start();
      verify(connProvider).stop();
      verify(connProvider).dispose();
    }
  }
}
