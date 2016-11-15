/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.deployment.model.api.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory.getDomainId;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.getDeploymentFile;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.getMuleDomainsDir;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.IOException;

public class DefaultDomainFactory implements DomainFactory {

  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final DomainManager domainManager;
  private final DomainDescriptorParser domainDescriptorParser;
  private final ClassLoaderRepository classLoaderRepository;
  private final ServiceRepository serviceRepository;

  private final ArtifactClassLoader containerClassLoader;
  private MuleContextListenerFactory muleContextListenerFactory;

  public DefaultDomainFactory(DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory,
                              DomainManager domainManager, ArtifactClassLoader containerClassLoader,
                              ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository) {
    this.classLoaderRepository = classLoaderRepository;
    checkArgument(domainManager != null, "Domain manager cannot be null");
    checkArgument(containerClassLoader != null, "Container classLoader cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");

    this.containerClassLoader = containerClassLoader;
    this.domainClassLoaderFactory = domainClassLoaderFactory;
    this.domainManager = domainManager;
    this.domainDescriptorParser = new DomainDescriptorParser();
    this.serviceRepository = serviceRepository;
  }

  public void setMuleContextListenerFactory(MuleContextListenerFactory muleContextListenerFactory) {
    this.muleContextListenerFactory = muleContextListenerFactory;
  }

  @Override
  public Domain createArtifact(File domainLocation) throws IOException {
    String domainName = domainLocation.getName();
    Domain domain = domainManager.getDomain(domainName);
    if (domain != null) {
      throw new IllegalArgumentException(format("Domain '%s'  already exists", domainName));
    }
    if (domainName.contains(" ")) {
      throw new IllegalArgumentException("Mule domain name may not contain spaces: " + domainName);
    }
    DomainDescriptor descriptor = findDomain(domainName);
    // TODO MULE-9653 - use the plugins class loader maps when plugins are allowed in domains
    DefaultMuleDomain defaultMuleDomain =
        new DefaultMuleDomain(descriptor, domainClassLoaderFactory.create(getDomainId(DEFAULT_DOMAIN_NAME), containerClassLoader,
                                                                          descriptor, emptyList()),
                              classLoaderRepository, serviceRepository);
    defaultMuleDomain.setMuleContextListener(muleContextListenerFactory.create(descriptor.getName()));
    DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
    domainManager.addDomain(domainWrapper);
    return domainWrapper;
  }

  private DomainDescriptor findDomain(String domainName) throws IOException {
    if (DEFAULT_DOMAIN_NAME.equals(domainName)) {
      return new EmptyDomainDescriptor(new File(getMuleDomainsDir(), DEFAULT_DOMAIN_NAME));
    }

    File domainFolder = getDomainFolder(domainName);
    final File deploymentFile = getDeploymentFile(domainFolder);

    DomainDescriptor descriptor;

    if (deploymentFile != null) {
      descriptor = domainDescriptorParser.parse(domainFolder, deploymentFile, domainName);
    } else {
      descriptor = new EmptyDomainDescriptor(new File(getMuleDomainsDir(), domainName));
    }

    return descriptor;
  }

  @Override
  public File getArtifactDir() {
    return getMuleDomainsDir();
  }

  public void dispose(DomainWrapper domain) {
    domainManager.removeDomain(domain.getArtifactName());
  }

  public void start(DomainWrapper domainWrapper) {
    domainManager.addDomain(domainWrapper);
  }
}
