/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.domain.Domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@link Domain} instances created on the container.
 */
public class DefaultDomainManager implements DomainRepository, DomainManager {

  private Map<String, Domain> domains = new HashMap();

  @Override
  public Domain getDomain(String name) {
    return domains.get(name);
  }

  @Override
  public void addDomain(Domain domain) {
    if (domains.containsKey(domain.getArtifactName())) {
      throw new IllegalArgumentException(format("Domain '%s' already exists", domain.getArtifactName()));
    }
    domains.put(domain.getArtifactName(), domain);
  }

  @Override
  public void removeDomain(String name) {
    domains.remove(name);
  }
}
