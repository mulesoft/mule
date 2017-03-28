/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.security.filters.MuleEncryptionEndpointSecurityFilter;

/**
 * Object factory for {@link MuleEncryptionEndpointSecurityFilter}.
 *
 * @since 4.0
 */
public class EncryptionSecurityFilterObjectFactory
    extends AbstractSecurityFilterObjectFactory<MuleEncryptionEndpointSecurityFilter> {

  private EncryptionStrategy strategy;

  public EncryptionSecurityFilterObjectFactory(EncryptionStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public MuleEncryptionEndpointSecurityFilter getFilter() {
    return new MuleEncryptionEndpointSecurityFilter(strategy);
  }

}
