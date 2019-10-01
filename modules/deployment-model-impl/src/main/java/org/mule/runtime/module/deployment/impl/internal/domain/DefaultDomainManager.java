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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages {@link Domain} instances created on the container.
 */
public class DefaultDomainManager implements DomainRepository, DomainManager {

  private Map<String, Domain> domainsByName = new HashMap<>();

  @Override
  public void addDomain(Domain domain) {
    String domainName = getDomainName(domain);
    if (domainsByName.containsKey(domainName)) {
      throw new IllegalArgumentException(format("Domain '%s' already exists", domainName));
    }

    domainsByName.put(domainName, domain);
  }

  @Override
  public void removeDomain(Domain domain) {
    String domainName = getDomainName(domain);
    domainsByName.remove(domainName);
  }

  @Override
  public Domain getDomain(String domainName) throws DomainNotFoundException {
    if (!domainsByName.containsKey(domainName)) {
      throw new DomainNotFoundException(domainName, domainsByName.keySet());
    }

    return domainsByName.get(domainName);
  }

  @Override
  public boolean contains(String name) {
    return domainsByName.containsKey(name);
  }

  @Override
  public Domain getCompatibleDomain(BundleDescriptor wantedDescriptor)
      throws DomainNotFoundException, AmbiguousDomainReferenceException {
    Set<String> foundDomainNames = new HashSet<>();
    Domain lastFoundDomain = null;

    for (Domain domain : domainsByName.values()) {
      BundleDescriptor currentDescriptor = domain.getDescriptor().getBundleDescriptor();
      if (currentDescriptor == null) {
        // Skip the domains without bundle descriptor (default, for example)
        continue;
      }

      if (isCompatibleBundle(currentDescriptor, wantedDescriptor)) {
        foundDomainNames.add(domain.getDescriptor().getName());
        lastFoundDomain = domain;
      }
    }

    if (foundDomainNames.size() < 1) {
      throw new DomainNotFoundException(wantedDescriptor.getArtifactFileName(), domainsByName.keySet());
    }

    if (foundDomainNames.size() > 1) {
      throw new AmbiguousDomainReferenceException(wantedDescriptor, foundDomainNames);
    }

    return lastFoundDomain;
  }

  @Override
  public boolean containsCompatible(BundleDescriptor descriptor) {
    for (Domain domain : domainsByName.values()) {
      if (isCompatibleBundle(domain.getDescriptor().getBundleDescriptor(), descriptor)) {
        return true;
      }
    }

    return false;
  }

  private static String getDomainName(Domain domain) {
    return domain.getDescriptor().getName();
  }

  /**
   * Determines if a bundle descriptor is compatible with another one.
   *
   * @param available bundle descriptor that is available to use.
   * @param expected bundle descriptor that is expected.
   * @return true if match in group and artifact id, have the same classifier and the versions are compatible, false otherwise.
   */
  public static boolean isCompatibleBundle(BundleDescriptor available, BundleDescriptor expected) {
    if (!available.getClassifier().equals(expected.getClassifier())) {
      return false;
    }

    if (!available.getGroupId().equals(expected.getGroupId())) {
      return false;
    }

    if (!available.getArtifactId().equals(expected.getArtifactId())) {
      return false;
    }

    return isCompatibleVersion(available.getVersion(), expected.getVersion());
  }

}
