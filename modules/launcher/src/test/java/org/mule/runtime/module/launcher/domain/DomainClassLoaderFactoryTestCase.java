/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.module.launcher.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.MuleSharedDomainClassLoader;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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

    assertThat(new DomainClassLoaderFactory(getClass().getClassLoader()).create(containerClassLoader, descriptor, emptyList())
        .getArtifactName(), is(DEFAULT_DOMAIN_NAME));
  }

  @Test
  public void createClassLoaderUsingCustomDomain() {
    String domainName = "custom-domain";
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    DomainDescriptor descriptor = getTestDescriptor(domainName);

    final ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader()).create(containerClassLoader, descriptor, emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactName(), equalTo(domainName));
  }

  @Test(expected = DeploymentException.class)
  public void validateDomainBeforeCreatingClassLoader() {
    DomainDescriptor descriptor = getTestDescriptor("someDomain");

    new DomainClassLoaderFactory(getClass().getClassLoader()).create(containerClassLoader, descriptor, emptyList());
  }

  @Test
  public void createClassLoaderFromDomainDescriptor() {
    String domainName = "descriptor-domain";
    DomainDescriptor descriptor = getTestDescriptor(domainName);
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    ArtifactClassLoader domainClassLoader =
        new DomainClassLoaderFactory(getClass().getClassLoader()).create(containerClassLoader, descriptor, emptyList());

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactName(), equalTo(domainName));
  }

  private DomainDescriptor getTestDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor();
    descriptor.setName(name);
    descriptor.setRedeploymentEnabled(false);
    return descriptor;
  }
}
