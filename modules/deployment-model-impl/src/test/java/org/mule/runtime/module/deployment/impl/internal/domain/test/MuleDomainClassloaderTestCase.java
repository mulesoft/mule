/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain.test;

import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.tck.mockito.answer.BuilderAnswer.BUILDER_ANSWER;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultMuleDomain;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleDomainClassloaderTestCase extends AbstractMuleTestCase {

  private static final ArtifactContextBuilder artifactContextBuilder = mock(ArtifactContextBuilder.class, BUILDER_ANSWER);

  @ClassRule
  public static TemporaryFolder artifactInstallationDirectory = new TemporaryFolder();

  private final ClassLoaderRepository domainClassLoaderRepository = mock(ClassLoaderRepository.class);
  private final DomainDescriptor domainDescriptor = mock(DomainDescriptor.class);
  private final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final List<ArtifactPlugin> artifactPlugins = emptyList();
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository = mock(ExtensionModelLoaderRepository.class);
  private final ClassLoader originalThreadClassloader = mock(ClassLoader.class);
  private final ClassLoader domainClassloader = mock(ClassLoader.class);
  private final ArtifactContext artifactContext = mock(ArtifactContext.class);
  private final MuleContext muleContext = mockMuleContext();
  private final MuleRegistry muleRegistry = mock(MuleRegistry.class);
  private final ArtifactStoppedPersistenceListener artifactStoppedPersistenceListener =
      mock(ArtifactStoppedPersistenceListener.class);
  private final LifecycleManager lifecycleManager = mock(LifecycleManager.class);
  private ClassLoader classloaderUsedInDispose;
  private Domain domain;

  @Before
  public void setUp() throws Exception {

    domain = new TestMuleDomain(domainDescriptor, artifactClassLoader, domainClassLoaderRepository, serviceRepository,
                                artifactPlugins, extensionModelLoaderRepository);
    currentThread().setContextClassLoader(originalThreadClassloader);
    when(domainDescriptor.getDeploymentProperties()).thenReturn(empty());
    when(domainDescriptor.getDataFolderName()).thenReturn("dataFolderName");
    when(artifactContextBuilder.build()).thenReturn(artifactContext);
    when(artifactContext.getMuleContext()).thenReturn(muleContext);
    when(muleContext.getLifecycleManager()).thenReturn(lifecycleManager);

    when(lifecycleManager.isDirectTransition(Stoppable.PHASE_NAME)).thenReturn(true);

    domain.init();
    when(artifactClassLoader.getClassLoader()).thenReturn(domainClassloader);
  }

  @Test
  public void disposeWithDomainClassloader() {
    doAnswer(invocation -> {
      classloaderUsedInDispose = currentThread().getContextClassLoader();
      return null;
    }).when(muleContext).dispose();

    doAnswer(invocation -> artifactStoppedPersistenceListener).when(muleRegistry).lookupObject(ARTIFACT_STOPPED_LISTENER);

    domain.dispose();

    assertThat(classloaderUsedInDispose, sameInstance(domainClassloader));
    assertThat(currentThread().getContextClassLoader(), is(originalThreadClassloader));
  }

  @Test
  public void stopWithDomainClassloader() throws Exception {
    doAnswer(invocation -> {
      classloaderUsedInDispose = currentThread().getContextClassLoader();
      return null;
    }).when(muleContext).stop();

    doAnswer(invocation -> artifactStoppedPersistenceListener).when(muleRegistry).lookupObject(ARTIFACT_STOPPED_LISTENER);

    domain.stop();

    assertThat(classloaderUsedInDispose, sameInstance(domainClassloader));
    assertThat(currentThread().getContextClassLoader(), is(originalThreadClassloader));
  }

  private static final class TestMuleDomain extends DefaultMuleDomain {

    public TestMuleDomain(DomainDescriptor descriptor, ArtifactClassLoader deploymentClassLoader,
                          ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository,
                          List<ArtifactPlugin> artifactPlugins, ExtensionModelLoaderRepository extensionModelLoaderRepository) {
      super(descriptor, deploymentClassLoader, classLoaderRepository, serviceRepository, artifactPlugins,
            extensionModelLoaderRepository, mock(MemoryManagementService.class),
            mock(ArtifactConfigurationProcessor.class));
    }

    @Override
    protected ArtifactContextBuilder getArtifactContextBuilder() {
      return artifactContextBuilder;
    }

    @Override
    protected File getArtifactInstallationDirectory() {
      return MuleDomainClassloaderTestCase.artifactInstallationDirectory.getRoot();
    }
  }

}
