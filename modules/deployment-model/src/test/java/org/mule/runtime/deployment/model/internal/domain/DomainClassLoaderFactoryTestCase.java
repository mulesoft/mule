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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory.getDomainId;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

public class DomainClassLoaderFactoryTestCase extends AbstractDomainTestCase {

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
    assertThat(new DomainClassLoaderFactory(getClass().getClassLoader())
        .create(artifactId, containerClassLoader, descriptor, emptyList())
        .getArtifactId(), is(artifactId));
  }

  @Test
  public void createClassLoaderUsingCustomDomain() {
    final String domainName = "custom-domain";
    final String artifactId = getDomainId(domainName);
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);

    final ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader()).create(null, containerClassLoader,
                                                                         descriptor,
                                                                         emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test(expected = DeploymentException.class)
  public void validateDomainBeforeCreatingClassLoader() {
    DomainDescriptor descriptor = getTestDescriptor("someDomain");

    new DomainClassLoaderFactory(getClass().getClassLoader()).create(null, containerClassLoader,
                                                                     descriptor, emptyList());
  }

  @Test
  public void createClassLoaderFromDomainDescriptor() {
    final String domainName = "descriptor-domain";
    final String artifactId = getDomainId(domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader()).create(null, containerClassLoader,
                                                                         descriptor,
                                                                         emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  private DomainDescriptor getTestDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor(name);
    descriptor.setRedeploymentEnabled(false);
    return descriptor;
  }
}
