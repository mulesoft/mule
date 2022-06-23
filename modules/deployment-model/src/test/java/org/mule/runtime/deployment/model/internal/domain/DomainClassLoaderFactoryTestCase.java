/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.deployment.model.api.builder.DeployableArtifactClassLoaderFactoryProvider.domainClassLoaderFactory;
import static org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory.getDomainId;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;

import static java.util.Collections.emptyList;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DomainClassLoaderFactoryTestCase extends AbstractDomainTestCase {

  @Rule
  public TemporaryFolder artifactLocation = new TemporaryFolder();

  public DomainClassLoaderFactoryTestCase() throws IOException {}

  @After
  public void tearDown() {
    deleteIfNeeded(getDomainsFolder());
    deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
  }

  private void deleteIfNeeded(File file) {
    if (file.exists()) {
      deleteQuietly(file);
    }
  }

  @Test
  public void createClassLoaderUsingDefaultDomain() {
    createDomainDir(MULE_DOMAIN_FOLDER, DEFAULT_DOMAIN_NAME);

    DomainDescriptor descriptor = getTestDescriptor(DEFAULT_DOMAIN_NAME);

    final String artifactId = getDomainId(DEFAULT_DOMAIN_NAME);
    assertThat(domainClassLoaderFactory(name -> getAppDataFolder(name))
        .create(artifactId, containerClassLoader, descriptor, emptyList())
        .getArtifactId(), is(artifactId));
  }

  @Test
  public void createClassLoaderUsingCustomDomain() {
    final String domainName = "custom-domain";
    final String artifactId = getDomainId(domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));

    final ArtifactClassLoader domainClassLoader = domainClassLoaderFactory(name -> getAppDataFolder(name)).create(null,
                                                                                                                  containerClassLoader,
                                                                                                                  descriptor,
                                                                                                                  emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test(expected = ArtifactActivationException.class)
  public void validateDomainBeforeCreatingClassLoader() {
    DomainDescriptor descriptor = getTestDescriptor("someDomain");
    descriptor.setRootFolder(new File("unexistent"));


    ModuleRepository moduleRepository = mock(ModuleRepository.class);
    DefaultArtifactClassLoaderResolver artifactClassLoaderResolver =
        new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepository,
                                                                          DomainClassLoaderFactoryTestCase.class
                                                                              .getClassLoader()),
                                               moduleRepository,
                                               new DefaultNativeLibraryFinderFactory(name -> getAppDataFolder(name)));
    DefaultDomainClassLoaderBuilder domainClassLoaderBuilder = new DefaultDomainClassLoaderBuilder(artifactClassLoaderResolver);

    domainClassLoaderBuilder.setArtifactDescriptor(descriptor);
    domainClassLoaderBuilder.build();
  }

  @Test
  public void createClassLoaderFromDomainDescriptor() {
    final String domainName = "descriptor-domain";
    final String artifactId = getDomainId(domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    ArtifactClassLoader domainClassLoader = domainClassLoaderFactory(name -> getAppDataFolder(name)).create(null,
                                                                                                            containerClassLoader,
                                                                                                            descriptor,
                                                                                                            emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test
  public void secondClassLoaderCreationReturnsCachedInstance() {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    DeployableArtifactClassLoaderFactory<DomainDescriptor> factory = domainClassLoaderFactory(name -> getAppDataFolder(name));

    final ArtifactClassLoader firstDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());
    final ArtifactClassLoader secondDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());

    assertThat(secondDomainClassLoader, is(firstDomainClassLoader));
  }

  @Test
  public void secondClassLoaderCreationDoesntReturnDisposedInstance() {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    DeployableArtifactClassLoaderFactory<DomainDescriptor> factory = domainClassLoaderFactory(name -> getAppDataFolder(name));

    final ArtifactClassLoader firstDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());
    firstDomainClassLoader.dispose();
    final ArtifactClassLoader secondDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());

    assertThat(secondDomainClassLoader, is(not(firstDomainClassLoader)));
  }

  @Test
  public void severalCreationsReturnCachedInstancesWhenNotDisposed() {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    DeployableArtifactClassLoaderFactory<DomainDescriptor> factory = domainClassLoaderFactory(name -> getAppDataFolder(name));

    final ArtifactClassLoader firstDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());
    assertThat(factory.create(null, containerClassLoader, descriptor, emptyList()), is(firstDomainClassLoader));
    assertThat(factory.create(null, containerClassLoader, descriptor, emptyList()), is(firstDomainClassLoader));

    firstDomainClassLoader.dispose();

    final ArtifactClassLoader secondDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());
    assertThat(secondDomainClassLoader, is(not(firstDomainClassLoader)));
    assertThat(factory.create(null, containerClassLoader, descriptor, emptyList()), is(secondDomainClassLoader));
    assertThat(factory.create(null, containerClassLoader, descriptor, emptyList()), is(secondDomainClassLoader));
  }

  private DomainDescriptor getTestDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor(name);
    descriptor.setRedeploymentEnabled(false);
    descriptor.setArtifactLocation(artifactLocation.getRoot());
    return descriptor;
  }

}
