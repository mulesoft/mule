/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.processor;

import org.mule.runtime.config.spring.privileged.dsl.processor.AbstractSecurityFilterObjectFactory;
import org.mule.runtime.core.security.UsernamePasswordAuthenticationFilter;

/**
 * Object factory for {@link UsernamePasswordAuthenticationFilter}.
 *
 * @since 4.0
 */
public class UsernamePasswordFilterObjectFactory
    extends AbstractSecurityFilterObjectFactory<UsernamePasswordAuthenticationFilter> {

  private String username;
  private String password;

  @Override
  public UsernamePasswordAuthenticationFilter getFilter() {
    UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();
    if (username != null) {
      filter.setUsername(username);
    }
    if (password != null) {
      filter.setPassword(password);
    }
    return filter;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
