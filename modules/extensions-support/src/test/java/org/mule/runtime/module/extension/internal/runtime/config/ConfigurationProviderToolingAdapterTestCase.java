/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.ImmutableList;

public class ConfigurationProviderToolingAdapterTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension> {

  private static final Class MODULE_CLASS = HeisenbergExtension.class;

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

  @Inject
  MetadataCacheIdGeneratorFactory<ComponentAst> cacheIdGeneratorFactory;

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

  @Test
  public void testCacheUsedForResolution() throws Exception {
    provider.initialise();
    provider.start();
    ((ConfigurationProviderToolingAdapter) provider).getMetadataKeys();
  }


}
