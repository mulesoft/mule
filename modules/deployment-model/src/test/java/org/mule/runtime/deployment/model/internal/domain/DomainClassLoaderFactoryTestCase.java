/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory.getDomainId;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

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
    assertThat(new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory())
        .create(artifactId, containerClassLoader, descriptor, emptyList())
        .getArtifactId(), is(artifactId));
  }

  @Test
  public void createClassLoaderUsingCustomDomain() {
    final String domainName = "custom-domain";
    final String artifactId = getDomainId(domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));

    final ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory()).create(null,
                                                                                                          containerClassLoader,
                                                                                                          descriptor,
                                                                                                          emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test(expected = DeploymentException.class)
  public void validateDomainBeforeCreatingClassLoader() {
    DomainDescriptor descriptor = getTestDescriptor("someDomain");
    descriptor.setRootFolder(new File("unexistent"));

    new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory()).create(null, containerClassLoader,
                                                                                                      descriptor, emptyList());
  }

  @Test
  public void createClassLoaderFromDomainDescriptor() {
    final String domainName = "descriptor-domain";
    final String artifactId = getDomainId(domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory()).create(null,
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
    DomainClassLoaderFactory factory = new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory());

    final ArtifactClassLoader firstDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());
    final ArtifactClassLoader secondDomainClassLoader = factory.create(null, containerClassLoader, descriptor, emptyList());

    assertThat(secondDomainClassLoader, is(firstDomainClassLoader));
  }

  @Test
  public void secondClassLoaderCreationDoesntReturnDisposedInstance() {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    DomainClassLoaderFactory factory = new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory());

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
    DomainClassLoaderFactory factory = new DomainClassLoaderFactory(getClass().getClassLoader(), getNativeLibraryFinderFactory());

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

  private NativeLibraryFinderFactory getNativeLibraryFinderFactory() {
    return new DefaultNativeLibraryFinderFactory();
  }
}
