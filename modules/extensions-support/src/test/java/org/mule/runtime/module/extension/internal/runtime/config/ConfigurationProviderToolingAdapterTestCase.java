/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class ConfigurationProviderToolingAdapterTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension> {

  private static final Class MODULE_CLASS = HeisenbergExtension.class;
  private static final int METADATA_ID_VALUE = 123456;
  private static final String METADATA_ID_STRING_VALUE = String.valueOf(METADATA_ID_VALUE);
  private static final String METADATA_CACHE_ID_GENERATOR_FACTORY_REGISTRY_NAME = "MetadataCacheIdGeneratorFactoryObject";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().silent();

  @Rule
  public ExpectedException expected = none();

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock
  private ConnectionProviderResolver connectionProviderResolver;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Parameterized.Parameter(0)
  public String caseName;

  @Parameterized.Parameter(1)
  public Map<String, Object> registryAdditions;

  @Parameterized.Parameter(2)
  public String configIdUsed;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Fallback cacheId due to missing service", emptyMap(), CONFIG_NAME},
        {"Fallback cacheId due to error",
            singletonMap(METADATA_CACHE_ID_GENERATOR_FACTORY_REGISTRY_NAME, new FailingTestMetadataCacheIdGeneratorFactory()),
            CONFIG_NAME},
        {"CacheId from MetadataCacheIdGenerator",
            singletonMap(METADATA_CACHE_ID_GENERATOR_FACTORY_REGISTRY_NAME, new TestMetadataCacheIdGeneratorFactory()),
            METADATA_ID_STRING_VALUE}
    });
  }

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

    visitableMock(operationModel);


    when(connectionProviderResolver.getResolverSet()).thenReturn(empty());
    when(connectionProviderResolver.resolve(any())).thenReturn(null);

    provider = new ConfigurationProviderToolingAdapter(CONFIG_NAME, extensionModel, configurationModel, configurationInstance,
                                                       new ReflectionCache(), muleContext);

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

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> registryObject = new HashMap<>();
    registryObject.putAll(super.getStartUpRegistryObjects());
    registryObject.putAll(registryAdditions);
    return registryObject;
  }

  @Test
  public void cacheId() {
    ConfigurationCacheIdResolver configurationCacheIdResolver = new ConfigurationCacheIdResolver(muleContext, provider);
    assertThat(configurationCacheIdResolver.getConfigurationCacheId(), is(configIdUsed));
  }

  private static class TestMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst> {

    @Override
    public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context, ComponentLocator<ComponentAst> locator) {
      return new TestMetadataCacheIdGenerator();
    }
  }

  private static class TestMetadataCacheIdGenerator implements MetadataCacheIdGenerator<ComponentAst> {

    @Override
    public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ComponentAst component) {
      return empty();
    }

    @Override
    public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ComponentAst component) {
      return empty();
    }

    @Override
    public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentAst component, String parameterName) {
      return empty();
    }

    @Override
    public Optional<MetadataCacheId> getIdForComponentMetadata(ComponentAst component) {
      return empty();
    }

    @Override
    public Optional<MetadataCacheId> getIdForMetadataKeys(ComponentAst component) {
      return empty();
    }

    @Override
    public Optional<MetadataCacheId> getIdForGlobalMetadata(ComponentAst component) {
      return of(new MetadataCacheId(METADATA_ID_VALUE, CONFIG_NAME));
    }
  }

  private static class FailingTestMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst> {

    @Override
    public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context, ComponentLocator<ComponentAst> locator) {
      throw new IllegalStateException("Illegal state!");
    }
  }

}
