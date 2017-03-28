/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.config;

import org.mule.runtime.config.spring.dsl.processor.AbstractSecurityFilterObjectFactory;
import org.mule.runtime.module.spring.security.AuthorizationFilter;

import java.util.Collection;

/**
 * Object factory for {@link AuthorizationFilter}.
 *
 * @since 4.0
 */
public class AuthorizationFilterObjectFactory extends AbstractSecurityFilterObjectFactory<AuthorizationFilter> {

  private Collection<String> requiredAuthorities;

  public AuthorizationFilterObjectFactory(Collection<String> requiredAuthorities) {
    this.requiredAuthorities = requiredAuthorities;
  }

  @Override
  public AuthorizationFilter getFilter() {
    return new AuthorizationFilter(requiredAuthorities);
  }

}
