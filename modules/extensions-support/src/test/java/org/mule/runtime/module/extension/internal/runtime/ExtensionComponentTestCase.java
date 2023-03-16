/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.api.config.MuleRuntimeFeature.START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

@SmallTest
@Issue("MULE-19815")
public class ExtensionComponentTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule rule = rule();

  @Mock
  private FeatureFlaggingService featureFlaggingService;

  @Mock
  private MuleApplicationClassLoader applicationClassLoader;

  private ExtensionComponent extensionComponent;

  private final AtomicReference<ClassLoader> executedClassloader = new AtomicReference<>();

  private MuleArtifactClassLoader artifactClassLoader;

  @Mock
  private ConfigurationProvider configurationProvider;

  private StaticValueResolver<ConfigurationProvider> configurationProviderResolver;

  @Before
  public void setUp() throws Exception {
    configurationProviderResolver = spy(new StaticValueResolver<>(configurationProvider));
    extensionComponent = new TestExtensionComponent(mock(ExtensionModel.class),
                                                    mock(ComponentModel.class), configurationProviderResolver,
                                                    mock(CursorProviderFactory.class), mock(ExtensionManager.class));

    RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
    when(regionClassLoader.getOwnerClassLoader()).thenReturn(applicationClassLoader);
    when(applicationClassLoader.getClassLoader()).thenReturn(applicationClassLoader);

    URL url = new URL("file:///app.txt");
    artifactClassLoader = new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class),
                                                      new URL[] {url}, regionClassLoader,
                                                      mock(ClassLoaderLookupPolicy.class));

    extensionComponent.classLoader = artifactClassLoader;

    MetadataCacheIdGeneratorFactory metadataCacheIdGeneratorFactory = mock(MetadataCacheIdGeneratorFactory.class);
    when(metadataCacheIdGeneratorFactory.create(any(), any())).thenReturn(null);
    extensionComponent.setCacheIdGeneratorFactory(of(metadataCacheIdGeneratorFactory));
    executedClassloader.set(null);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(FEATURE_FLAGGING_SERVICE_KEY, featureFlaggingService);
  }

  @Test
  public void testLoadingWithComposite() throws MuleException {
    when(featureFlaggingService.isEnabled(START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER)).thenReturn(false);
    initialiseIfNeeded(extensionComponent, muleContext);
    extensionComponent.start();
    assertThat(executedClassloader.get(), instanceOf(CompositeClassLoader.class));
    assertThat(((CompositeClassLoader) executedClassloader.get()).getDelegates().size(), is(2));
    assertThat(((CompositeClassLoader) executedClassloader.get()).getDelegates().get(0), is(artifactClassLoader));
    assertThat(((CompositeClassLoader) executedClassloader.get()).getDelegates().get(1), is(applicationClassLoader));
  }

  @Test
  public void testLoadingWithArtifact() throws MuleException {
    when(featureFlaggingService.isEnabled(START_EXTENSION_COMPONENTS_WITH_ARTIFACT_CLASSLOADER)).thenReturn(true);
    initialiseIfNeeded(extensionComponent, muleContext);
    extensionComponent.start();
    assertThat(executedClassloader.get(), instanceOf(MuleArtifactClassLoader.class));
    assertThat(executedClassloader.get(), is(artifactClassLoader));
  }

  @Test
  @Issue("W-12271999")
  @Description("Non functional test case to verify that the resolution of a static config provider resolver is cached. This is testing an optimization, it is not a functional requirement.")
  public void staticConfigProviderResolutionIsCached() throws MuleException {
    initialiseIfNeeded(extensionComponent, muleContext);
    extensionComponent.start();

    // We clear the invocations here because we are not interested in invocations during the start phase
    clearInvocations(configurationProviderResolver);

    extensionComponent.getConfigurationProvider();
    extensionComponent.getConfigurationProvider();
    verify(configurationProviderResolver, atMost(1)).resolve(any());
  }

  @Test
  @Issue("W-12271999")
  @Description("Non functional test case to verify that the retrieval of a configuration from a static provider is cached. This is testing an optimization, it is not a functional requirement.")
  public void staticConfigInstanceIsCached() throws MuleException {
    initialiseIfNeeded(extensionComponent, muleContext);
    extensionComponent.start();

    // We clear the invocations here because we are not interested in invocations during the start phase
    clearInvocations(configurationProvider);

    extensionComponent.getStaticConfiguration();
    extensionComponent.getStaticConfiguration();
    verify(configurationProvider, atMost(1)).get(any());
  }

  @Test
  @Issue("W-12271999")
  @Description("Non functional test case to verify that the computation of usesDynamicConfiguration is cached. This is testing an optimization, it is not a functional requirement.")
  public void usesDynamicConfigurationIsCached() throws MuleException {
    initialiseIfNeeded(extensionComponent, muleContext);
    extensionComponent.start();

    // We clear the invocations here because we are not interested in invocations during the start phase
    clearInvocations(configurationProviderResolver);
    clearInvocations(configurationProvider);

    extensionComponent.usesDynamicConfiguration();
    extensionComponent.usesDynamicConfiguration();
    verify(configurationProviderResolver, atMost(1)).resolve(any());
    verify(configurationProvider, atMost(1)).get(any());
  }

  private final class TestExtensionComponent extends ExtensionComponent {

    TestExtensionComponent(ExtensionModel extensionModel,
                           ComponentModel componentModel,
                           ValueResolver<ConfigurationProvider> configurationProviderResolver,
                           CursorProviderFactory cursorProviderFactory,
                           ExtensionManager extensionManager) {
      super(extensionModel, componentModel, configurationProviderResolver, cursorProviderFactory, extensionManager);
    }

    @Override
    protected void doInitialise() throws InitialisationException {}

    @Override
    protected void doStart() throws MuleException {
      executedClassloader.set(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected void doStop() throws MuleException {}

    @Override
    protected void doDispose() {}

    @Override
    protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

    @Override
    protected ParameterValueResolver getParameterValueResolver() {
      return null;
    }
  }
}
