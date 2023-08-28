/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.deployment.model.api.domain.Domain;

/**
 * Tracks {@link Domain} instances deployed on the container
 */
public interface DomainManager extends DomainRepository {

  /**
   * Adds a new domain
   *
   * @param domain domain to add.
   * @throws IllegalArgumentException if the domain is already added.
   */
  void addDomain(Domain domain);

  /**
   * Removes a domain
   *
   * @param domain domain to remove.
   */
  void removeDomain(Domain domain);

}
