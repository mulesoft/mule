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

  private Map<SemVerBundleDescriptorWrapper, Domain> domainsByDescriptor = new HashMap<>();

  @Override
  public Domain getDomain(BundleDescriptor bundleDescriptor) throws DomainNotFoundException, IncompatibleDomainVersionException {
    SemVerBundleDescriptorWrapper bundleDescriptorWrapper = new SemVerBundleDescriptorWrapper(bundleDescriptor);
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
  public boolean contains(BundleDescriptor descriptor) {
    SemVerBundleDescriptorWrapper bundleDescriptorWrapper = new SemVerBundleDescriptorWrapper(descriptor);
    if (domainsByDescriptor.containsKey(bundleDescriptorWrapper)) {
      Domain domain = domainsByDescriptor.get(bundleDescriptorWrapper);
      String availableVersion = domain.getDescriptor().getBundleDescriptor().getVersion();
      String expectedVersion = descriptor.getVersion();
      return isCompatibleVersion(availableVersion, expectedVersion);
    }

    return false;
  }

  @Override
  public void addDomain(Domain domain) {
    final SemVerBundleDescriptorWrapper bundleDescriptorWrapper = new SemVerBundleDescriptorWrapper(domain.getDescriptor());
    Domain foundDomain = domainsByDescriptor.get(bundleDescriptorWrapper);
    if (foundDomain != null) {
      throw new IllegalArgumentException(getDomainAlreadyExistsErrorMessage(getDomainName(domain), getDomainName(foundDomain)));
    }
    domainsByDescriptor.put(bundleDescriptorWrapper, domain);
  }

  @Override
  public void removeDomain(Domain domain) {
    final SemVerBundleDescriptorWrapper bundleDescriptorWrapper = new SemVerBundleDescriptorWrapper(domain.getDescriptor());
    domainsByDescriptor.remove(bundleDescriptorWrapper);
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
