/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@link Domain} instances created on the container.
 */
public class DefaultDomainManager implements DomainRepository, DomainManager {

  private Map<BundleDescriptorWrapper, Domain> domainsByDescriptor = new HashMap<>();
  private Map<String, BundleDescriptorWrapper> descriptorsByName = new HashMap<>();

  @Override
  public Domain getDomain(BundleDescriptor bundleDescriptor) throws DomainNotFoundException, IncompatibleDomainVersionException {
    BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(bundleDescriptor);
    Domain domain = domainsByDescriptor.get(bundleDescriptorWrapper);
    if (domain == null) {
      throw new DomainNotFoundException(bundleDescriptor.getArtifactFileName(), domainsByDescriptor.keySet());
    }

    String availableVersion = domain.getDescriptor().getBundleDescriptor().getVersion();
    String expectedVersion = bundleDescriptor.getVersion();
    if (isCompatibleVersion(availableVersion, expectedVersion)) {
      return domain;
    } else {
      throw new IncompatibleDomainVersionException(bundleDescriptor.getArtifactFileName(), availableVersion);
    }
  }

  @Override
  public Domain getDomain(String domainName) throws DomainNotFoundException {
    BundleDescriptorWrapper bundleDescriptorWrapper = descriptorsByName.get(domainName);
    if (bundleDescriptorWrapper == null) {
      throw new DomainNotFoundException(domainName, domainsByDescriptor.keySet());
    }
    Domain domain = domainsByDescriptor.get(bundleDescriptorWrapper);
    if (domain == null) {
      throw new DomainNotFoundException(domainName, domainsByDescriptor.keySet());
    }
    return domain;
  }

  @Override
  public boolean contains(BundleDescriptor descriptor) {
    BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(descriptor);
    if (domainsByDescriptor.containsKey(bundleDescriptorWrapper)) {
      Domain domain = domainsByDescriptor.get(bundleDescriptorWrapper);
      String availableVersion = domain.getDescriptor().getBundleDescriptor().getVersion();
      String expectedVersion = descriptor.getVersion();
      return isCompatibleVersion(availableVersion, expectedVersion);
    }

    return contains(descriptor.getArtifactFileName());
  }

  @Override
  public boolean contains(String domainName) {
    if (!descriptorsByName.containsKey(domainName)) {
      return false;
    }
    return domainsByDescriptor.containsKey(descriptorsByName.get(domainName));
  }

  @Override
  public void addDomain(Domain domain) {
    final BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(domain.getDescriptor());
    Domain foundDomain = domainsByDescriptor.get(bundleDescriptorWrapper);
    if (foundDomain != null) {
      throw new IllegalArgumentException(getDomainAlreadyExistsErrorMessage(getDomainName(domain), getDomainName(foundDomain)));
    }
    domainsByDescriptor.put(bundleDescriptorWrapper, domain);
    addNameMappings(domain, bundleDescriptorWrapper);
  }

  @Override
  public void removeDomain(Domain domain) {
    final BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(domain.getDescriptor());
    domainsByDescriptor.remove(bundleDescriptorWrapper);
    removeNameMappings(domain);
  }

  private void addNameMappings(Domain domain, BundleDescriptorWrapper bundleDescriptorWrapper) {
    BundleDescriptor bundleDescriptor = domain.getDescriptor().getBundleDescriptor();
    if (bundleDescriptor != null) {
      String bundleName = bundleDescriptor.getArtifactId();
      descriptorsByName.put(bundleName, bundleDescriptorWrapper);
    }

    String artifactName = domain.getArtifactName();
    descriptorsByName.put(artifactName, bundleDescriptorWrapper);
  }

  private void removeNameMappings(Domain domain) {
    BundleDescriptor bundleDescriptor = domain.getDescriptor().getBundleDescriptor();
    if (bundleDescriptor != null) {
      descriptorsByName.remove(bundleDescriptor.getArtifactId());
    }
    descriptorsByName.remove(domain.getArtifactName());
  }

  private static String getDomainName(Domain domain) {
    DomainDescriptor domainDescriptor = domain.getDescriptor();
    BundleDescriptor bundleDescriptor = domainDescriptor.getBundleDescriptor();
    if (bundleDescriptor == null) {
      return domainDescriptor.getName();
    } else {
      return bundleDescriptor.getArtifactFileName();
    }
  }

  private static String getDomainAlreadyExistsErrorMessage(String requestedDomainName, String foundDomainName) {
    return format("Trying to add domain '%s', but a domain named '%s' was found", requestedDomainName, foundDomainName);
  }
}
