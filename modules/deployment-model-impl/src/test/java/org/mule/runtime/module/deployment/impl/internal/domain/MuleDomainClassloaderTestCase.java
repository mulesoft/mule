/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.mockito.answer.BuilderAnswer.BUILDER_ANSWER;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleDomainClassloaderTestCase extends AbstractMuleTestCase {

  private static final ArtifactContextBuilder artifactContextBuilder = mock(ArtifactContextBuilder.class, BUILDER_ANSWER);
  private final ClassLoaderRepository domainClassLoaderRepository = mock(ClassLoaderRepository.class);
  private final DomainDescriptor domainDescriptor = mock(DomainDescriptor.class);
  private final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final List<ArtifactPlugin> artifactPlugins = emptyList();
  private final ExtensionModelLoaderManager extensionModelLoaderManager = mock(ExtensionModelLoaderManager.class);
  private final ClassLoader originalThreadClassloader = mock(ClassLoader.class);
  private final ClassLoader domainClassloader = mock(ClassLoader.class);
  private final ArtifactContext artifactContext = mock(ArtifactContext.class);
  private final MuleContext muleContext = mock(MuleContext.class);
  private ClassLoader classloaderUsedInDispose;
  private Domain domain;

  @Before
  public void setUp() throws Exception {

    domain = new TestMuleDomain(domainDescriptor, artifactClassLoader, domainClassLoaderRepository, serviceRepository,
                                artifactPlugins, extensionModelLoaderManager);
    currentThread().setContextClassLoader(originalThreadClassloader);
    when(domainDescriptor.getDeploymentProperties()).thenReturn(empty());
    when(domainDescriptor.getDataFolderName()).thenReturn("dataFolderName");
    when(artifactContextBuilder.build()).thenReturn(artifactContext);
    when(artifactContext.getMuleContext()).thenReturn(muleContext);
    domain.init();
    when(artifactClassLoader.getClassLoader()).thenReturn(domainClassloader);
  }

  @Test
  public void disposeWithDomainClassloader() {
    doAnswer(invocation -> {
      classloaderUsedInDispose = currentThread().getContextClassLoader();
      return null;
    }).when(muleContext).dispose();

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

    domain.stop();

    assertThat(classloaderUsedInDispose, sameInstance(domainClassloader));
    assertThat(currentThread().getContextClassLoader(), is(originalThreadClassloader));
  }

  private static final class TestMuleDomain extends DefaultMuleDomain {

    public TestMuleDomain(DomainDescriptor descriptor, ArtifactClassLoader deploymentClassLoader,
                          ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository,
                          List<ArtifactPlugin> artifactPlugins, ExtensionModelLoaderManager extensionModelLoaderManager) {
      super(descriptor, deploymentClassLoader, classLoaderRepository, serviceRepository, artifactPlugins,
            extensionModelLoaderManager);
    }

    @Override
    protected ArtifactContextBuilder getArtifactContextBuilder() {
      return artifactContextBuilder;
    }

    @Override
    protected File getArtifactInstallationDirectory() {
      return mock(File.class);
    }
  }

}
