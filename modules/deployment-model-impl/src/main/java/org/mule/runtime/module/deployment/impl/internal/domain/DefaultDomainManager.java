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
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@link Domain} instances created on the container.
 */
public class DefaultDomainManager implements DomainRepository, DomainManager {

  private Map<BundleDescriptorWrapper, Domain> domainsByDescriptor = new HashMap<>();
  private Map<String, Domain> domainsByName = new HashMap<>();

  @Override
  public Domain getDomain(BundleDescriptor bundleDescriptor) throws DomainNotFoundException, IncompatibleDomainVersionException {
    BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(bundleDescriptor);
    Domain domain = domainsByDescriptor.get(bundleDescriptorWrapper);
    if (domain == null) {
      throw new DomainNotFoundException(bundleDescriptor.getArtifactFileName());
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
    Domain domain = domainsByName.get(domainName);
    if (domain == null) {
      throw new DomainNotFoundException(domainName);
    }
    return domain;
  }

  @Override
  public void addDomain(Domain domain) {
    BundleDescriptor bundleDescriptor = domain.getDescriptor().getBundleDescriptor();
    String domainName;

    if (bundleDescriptor != null) {
      final BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(bundleDescriptor);
      if (domainsByDescriptor.containsKey(bundleDescriptorWrapper)) {
        throw new IllegalArgumentException(format("Domain '%s' already exists", domain.getArtifactName()));
      }
      domainsByDescriptor.put(bundleDescriptorWrapper, domain);
      domainName = bundleDescriptor.getArtifactFileName();
    } else {
      domainName = domain.getDescriptor().getName();
    }

    domainsByName.put(domainName, domain);
  }

  @Override
  public void removeDomain(Domain domain) {
    BundleDescriptor bundleDescriptor = domain.getDescriptor().getBundleDescriptor();
    String domainName;

    if (bundleDescriptor != null) {
      final BundleDescriptorWrapper bundleDescriptorWrapper = new BundleDescriptorWrapper(bundleDescriptor);
      domainsByDescriptor.remove(bundleDescriptorWrapper);
      domainName = bundleDescriptor.getArtifactFileName();
    } else {
      domainName = domain.getDescriptor().getName();
    }

    domainsByName.remove(domainName);
  }
}
