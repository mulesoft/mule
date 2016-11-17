/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.deployment.model.api.domain.Domain;

/**
 * Provides access to {@link Domain} available on the container
 */
public interface DomainRepository {

  /**
   * @param name domain name to find. Non empty.
   * @return a {@link Domain} corresponding to the given name or null is no such domain exists.
   */
  Domain getDomain(String name);

}
