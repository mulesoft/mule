/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
