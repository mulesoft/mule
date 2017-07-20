/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.processor;

import org.mule.runtime.config.spring.api.dsl.processor.AbstractSecurityFilterObjectFactory;
import org.mule.runtime.core.api.security.SecurityFilter;

/**
 * Object factory for custom {@link SecurityFilter}.
 *
 * @since 4.0
 */
public class CustomSecurityFilterObjectFactory extends AbstractSecurityFilterObjectFactory<SecurityFilter> {

  private final SecurityFilter filter;

  public CustomSecurityFilterObjectFactory(SecurityFilter filter) {
    this.filter = filter;
  }

  @Override
  public SecurityFilter getFilter() {
    return filter;
  }

}
