/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Manages {@link Domain} instances created on the container.
 */
public class DefaultDomainManager implements DomainRepository, DomainManager {

  private static final Logger LOGGER = getLogger(DefaultDomainManager.class);

  private Map<String, Domain> domainsByName = new HashMap<>();

  @Override
  public void addDomain(Domain domain) {
    String domainName = getDomainName(domain);
    if (domainsByName.containsKey(domainName)) {
      throw new IllegalArgumentException(format("Domain '%s' already exists", domainName));
    }

    domainsByName.put(domainName, domain);

    LOGGER.error("Domain " + domainName + " added");
  }

  @Override
  public void removeDomain(Domain domain) {
    // TODO: Should check existence?
    String domainName = getDomainName(domain);
    //if (!domainsByName.containsKey(domainName)) {
    //  // TODO: Use specific exception
    //  throw new IllegalArgumentException(format("Domain '%s' doesn't exist", domainName));
    //}

    domainsByName.remove(domainName);

    LOGGER.error("Domain " + domainName + " removed");
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
  public Domain getCompatibleDomain(BundleDescriptor wantedDescriptor) throws DomainNotFoundException {
    Domain foundDomain = null;

    for (Domain domain : domainsByName.values()) {
      BundleDescriptor currentDescriptor = domain.getDescriptor().getBundleDescriptor();
      if (currentDescriptor == null) {
        // Skip the domains without bundle descriptor (default, for example)
        continue;
      }

      if (areCompatible(currentDescriptor, wantedDescriptor)) {
        if (foundDomain != null) {
          // TODO: Use specific exception
          throw new IllegalArgumentException("More than one compatible domain were found");
        }
        foundDomain = domain;
      }
    }

    if (foundDomain == null) {
      throw new DomainNotFoundException(wantedDescriptor.getArtifactFileName(), domainsByName.keySet());
    }

    return foundDomain;
  }

  public boolean areCompatible(BundleDescriptor available, BundleDescriptor expected) {
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

  @Override
  public boolean containsCompatible(BundleDescriptor descriptor) {
    for (Domain domain : domainsByName.values()) {
      if (areCompatible(domain.getDescriptor().getBundleDescriptor(), descriptor)) {
        return true;
      }
    }

    return false;
  }

  private static String getDomainName(Domain domain) {
    return domain.getDescriptor().getName();
  }

}
