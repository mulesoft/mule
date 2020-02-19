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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigurationProviderTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension> {

  private static final Class MODULE_CLASS = HeisenbergExtension.class;

  @Rule
  public ExpectedException expected = none();

  @Mock
  private ResolverSet resolverSet;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ResolverSetResult resolverSetResult;

  @Mock
  private ConnectionProviderResolver connectionProviderResolver;

  @Mock
  private ExpressionManager expressionManager;

  private ExpirationPolicy expirationPolicy;

  @Override
  @Before
  public void before() throws Exception {
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getSourceModels()).thenReturn(ImmutableList.of());

    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionModel.getSourceModels()).thenReturn(ImmutableList.of());
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(operationModel.requiresConnection()).thenReturn(true);
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(configurationModel.getSourceModels()).thenReturn(ImmutableList.of());

    when(expressionManager.openSession(any())).thenReturn(mock(ExpressionManagerSession.class));

    ValueResolvingContext ctx = ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build();
    when(resolverSet.resolve(ctx)).thenReturn(resolverSetResult);
    when(resolverSetResult.asMap()).thenReturn(new HashMap<>());
    visitableMock(operationModel);

    expirationPolicy = new ImmutableExpirationPolicy(5, MINUTES, timeSupplier);

    when(connectionProviderResolver.getResolverSet()).thenReturn(empty());
    when(connectionProviderResolver.resolve(any())).thenReturn(null);

    muleContext.setExtensionManager(new DefaultExtensionManager());
    provider = new DynamicConfigurationProvider(CONFIG_NAME, extensionModel, configurationModel, resolverSet,
                                                connectionProviderResolver, expirationPolicy, new ReflectionCache(),
                                                expressionManager, muleContext);

    super.before();
    provider.initialise();
    provider.start();
  }

  @After
  public void after() throws MuleException {
    stopIfNecessary();
    disposeIfNecessary();
  }

  private void disposeIfNecessary() {
    if (isValidTransition(Disposable.PHASE_NAME)) {
      provider.dispose();
    }
  }

  private void stopIfNecessary() throws MuleException {
    if (isValidTransition(Stoppable.PHASE_NAME)) {
      provider.stop();
    }
  }

  private boolean isValidTransition(String phaseName) {
    return provider.lifecycleManager.getState().isValidTransition(phaseName);
  }

  @Test
  public void resolveCached() throws Exception {
    final int count = 10;
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    for (int i = 1; i < count; i++) {
      assertThat(provider.get(event).getValue(), is(sameInstance(config)));
    }

    ValueResolvingContext ctx = ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build();
    verify(resolverSet, times(count)).resolve(ctx);
  }

  @Test
  public void resolveCachedWithProviderParams() throws Exception {
    ResolverSet providerResolverSet = mock(ResolverSet.class);
    when(connectionProviderResolver.getResolverSet()).thenReturn(of(providerResolverSet));
    when(connectionProviderResolver.getObjectBuilder()).thenReturn(empty());
    ValueResolvingContext ctx = ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build();
    when(providerResolverSet.resolve(ctx)).thenReturn(resolverSetResult);

    final int count = 10;
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    for (int i = 1; i < count; i++) {
      assertThat(provider.get(event).getValue(), is(sameInstance(config)));
    }

    verify(providerResolverSet, times(count))
        .resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build());
    verify(resolverSet, times(count))
        .resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build());
  }

  @Test
  public void resolveProviderParamsDifferentInstance() throws Exception {
    HeisenbergExtension config = (HeisenbergExtension) provider.get(event).getValue();
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());

    ResolverSet providerResolverSet = mock(ResolverSet.class);
    when(connectionProviderResolver.getResolverSet()).thenReturn(of(providerResolverSet));
    when(connectionProviderResolver.getObjectBuilder()).thenReturn(empty());
    when(providerResolverSet.resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build()))
        .thenReturn(mock(ResolverSetResult.class));
    assertThat(provider.get(event).getValue(), is(not(sameInstance(config))));

    verify(resolverSet, times(2))
        .resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build());
    verify(providerResolverSet, times(1))
        .resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build());
    verify(connectionProviderResolver, times(2))
        .resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build());
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

    List<Object> configs = expired.stream().map(ConfigurationInstance::getValue).collect(toImmutableList());
    assertThat(configs, containsInAnyOrder(instance1, instance2));
  }

  @Test
  public void configurationInstanceIsRemovedFromLifecycleTrackingAfterExpired() throws Exception {
    HeisenbergExtension instance = (HeisenbergExtension) provider.get(event).getValue();

    DynamicConfigurationProvider provider = (DynamicConfigurationProvider) this.provider;

    timeSupplier.move(10, MINUTES);

    List<ConfigurationInstance> expired = provider.getExpired();
    assertThat(expired.isEmpty(), is(false));

    provider.stop();
    provider.dispose();

    assertThat(instance.getStop(), is(0));
    assertThat(instance.getDispose(), is(0));
  }

  @Test
  public void configurationInstanceFollowsLifecycleTrakingWhenNotExpired() throws Exception {
    HeisenbergExtension instance = (HeisenbergExtension) provider.get(event).getValue();

    DynamicConfigurationProvider provider = (DynamicConfigurationProvider) this.provider;

    timeSupplier.move(1, MINUTES);

    List<ConfigurationInstance> expired = provider.getExpired();
    assertThat(expired.isEmpty(), is(true));

    provider.stop();
    provider.dispose();

    assertThat(instance.getStop(), is(1));
    assertThat(instance.getDispose(), is(1));
  }

  private HeisenbergExtension makeAlternateInstance() throws Exception {
    ResolverSetResult alternateResult = mock(ResolverSetResult.class, Mockito.RETURNS_DEEP_STUBS);
    when(alternateResult.asMap()).thenReturn(new HashMap<>());
    mockConfigurationInstance(configurationModel, MODULE_CLASS.newInstance());
    when(resolverSet.resolve(ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build()))
        .thenReturn(alternateResult);

    return (HeisenbergExtension) provider.get(event).getValue();
  }

  @Test
  public void resolveDynamicConfigWithEquivalentEvent() throws Exception {
    assertSameInstancesResolved();
  }

  @Test
  public void resolveDynamicConfigWithDifferentEvent() throws Exception {
    Object config1 = provider.get(event);

    ValueResolvingContext ctx = ValueResolvingContext.builder(event).withExpressionManager(expressionManager).build();
    when(resolverSet.resolve(ctx)).thenReturn(mock(ResolverSetResult.class));
    Object config2 = provider.get(event);

    assertThat(config1, is(not(sameInstance(config2))));
  }

  @Test
  public void configFailsOnInitialize() throws Exception {
    final Lifecycle connProvider = mock(Lifecycle.class, withSettings().extraInterfaces(ConnectionProvider.class));
    final String expectedExceptionMessage = "Init failed!";
    doThrow(new RuntimeException(expectedExceptionMessage)).when(connProvider).initialise();

    when(connectionProviderResolver.resolve(any())).thenReturn(new Pair<>(connProvider, mock(ResolverSetResult.class)));

    expected.expectCause(both(hasMessage(equalTo(expectedExceptionMessage))).and(is(instanceOf(InitialisationException.class))));

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
