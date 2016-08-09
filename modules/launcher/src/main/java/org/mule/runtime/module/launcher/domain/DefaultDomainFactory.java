/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.module.launcher.artifact.ArtifactFactoryUtils.getDeploymentFile;
import static org.mule.runtime.module.launcher.domain.Domain.DEFAULT_DOMAIN_NAME;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptor;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptorParser;
import org.mule.runtime.module.launcher.descriptor.EmptyDomainDescriptor;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;

public class DefaultDomainFactory implements DomainFactory {

  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final DomainManager domainManager;
  private final DomainDescriptorParser domainDescriptorParser;

  protected DeploymentListener deploymentListener;
  private final ArtifactClassLoader containerClassLoader;

  public DefaultDomainFactory(DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory,
                              DomainManager domainManager, ArtifactClassLoader containerClassLoader) {
    checkArgument(domainManager != null, "Domain manager cannot be null");
    checkArgument(containerClassLoader != null, "Container classLoader cannot be null");
    this.containerClassLoader = containerClassLoader;
    this.domainClassLoaderFactory = domainClassLoaderFactory;
    this.domainManager = domainManager;
    this.domainDescriptorParser = new DomainDescriptorParser();
  }

  public void setDeploymentListener(DeploymentListener deploymentListener) {
    this.deploymentListener = deploymentListener;
  }

  @Override
  public Domain createArtifact(String artifactName) throws IOException {
    Domain domain = domainManager.getDomain(artifactName);
    if (domain != null) {
      throw new IllegalArgumentException(format("Domain '%s'  already exists", artifactName));
    }
    if (artifactName.contains(" ")) {
      throw new IllegalArgumentException("Mule domain name may not contain spaces: " + artifactName);
    }
    DomainDescriptor descriptor = findDomain(artifactName);
    // TODO MULE-9653 - use the plugins class loader maps when plugins are allowed in domains
    DefaultMuleDomain defaultMuleDomain =
        new DefaultMuleDomain(descriptor, domainClassLoaderFactory.create(containerClassLoader, descriptor, emptyList()));
    defaultMuleDomain.setDeploymentListener(deploymentListener);
    DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
    domainManager.addDomain(domainWrapper);
    return domainWrapper;
  }

  private DomainDescriptor findDomain(String domainName) throws IOException {
    if (DEFAULT_DOMAIN_NAME.equals(domainName)) {
      return new EmptyDomainDescriptor(DEFAULT_DOMAIN_NAME);
    }

    final File deploymentFile = getDeploymentFile(getDomainFolder(domainName));

    DomainDescriptor descriptor;

    if (deploymentFile != null) {
      descriptor = domainDescriptorParser.parse(deploymentFile, domainName);
    } else {
      descriptor = new EmptyDomainDescriptor(domainName);
    }

    return descriptor;
  }

  @Override
  public File getArtifactDir() {
    return MuleContainerBootstrapUtils.getMuleDomainsDir();
  }

  public void dispose(DomainWrapper domain) {
    domainManager.removeDomain(domain.getArtifactName());
  }

  public void start(DomainWrapper domainWrapper) {
    domainManager.addDomain(domainWrapper);
  }
}
