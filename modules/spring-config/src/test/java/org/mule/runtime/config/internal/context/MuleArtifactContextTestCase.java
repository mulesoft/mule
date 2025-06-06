/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.config.MuleRuntimeFeature.VALIDATE_APPLICATION_MODEL_WITH_REGION_CLASSLOADER;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.util.MuleAstUtils;
import org.mule.runtime.ast.api.validation.ArtifactAstValidatorBuilder;
import org.mule.runtime.ast.internal.validation.DefaultValidatorBuilder;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("W-10808757")
@Story(APP_DEPLOYMENT)
public class MuleArtifactContextTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private MuleContext mockMuleContext;
  private final ClassLoader executionClassloader = mock(ClassLoader.class);
  private final ClassLoader parentClassloader = mock(ClassLoader.class);
  private final DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(null);
  private ArtifactAstValidatorBuilder astValidatorBuilder;

  @Before
  public void before() throws MuleException {
    astValidatorBuilder = spy(new DefaultValidatorBuilder());
    mockStatic(MuleAstUtils.class);
    when(validatorBuilder()).thenReturn(astValidatorBuilder);
    when(emptyArtifact()).thenCallRealMethod();
    mockMuleContext = mockContextWithServices();
    mockMuleContext.getInjector().inject(this);
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(executionClassloader);
    when(executionClassloader.getParent()).thenReturn(parentClassloader);
  }

  @Test
  public void testValidationWithFFEnabled() {
    createMuleArtifactContextStub(beanFactory, mockFF(true));
    verify(astValidatorBuilder, times(1)).withArtifactRegionClassLoader(parentClassloader);
  }

  @Test
  public void testValidationWithFFDisabled() {
    createMuleArtifactContextStub(beanFactory, mockFF(false));
    verify(astValidatorBuilder, times(1)).withArtifactRegionClassLoader(executionClassloader);
  }

  private FeatureFlaggingService mockFF(boolean flag) {
    FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);
    when(featureFlaggingService.isEnabled(VALIDATE_APPLICATION_MODEL_WITH_REGION_CLASSLOADER)).thenReturn(flag);
    return featureFlaggingService;
  }

  private MuleArtifactContext createMuleArtifactContextStub(DefaultListableBeanFactory mockedBeanFactory,
                                                            FeatureFlaggingService featureFlaggingService) {

    MuleArtifactContext muleArtifactContext =
        new MuleArtifactContext(mockMuleContext, emptyArtifact(), empty(),
                                new BaseConfigurationComponentLocator(),
                                new ContributedErrorTypeRepository(), new ContributedErrorTypeLocator(),
                                emptyMap(), APP,
                                new ComponentBuildingDefinitionRegistry(),
                                mock(MemoryManagementService.class),
                                featureFlaggingService, mock(ExpressionLanguageMetadataService.class)) {

          @Override
          protected DefaultListableBeanFactory createBeanFactory() {
            return mockedBeanFactory;
          }

          @Override
          protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean invocation here
          }

          @Override
          protected void registerListeners() {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishRefresh() {
            // Bean factory is mocked, so no nothing to do here
          }
        };
    muleArtifactContext.refresh();
    return muleArtifactContext;
  }
}
