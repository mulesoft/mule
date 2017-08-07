/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;

/**
 * Trivial {@link SecurityContext} implementation which simply holds the {@link Authentication} object.
 */
public class DefaultSecurityContext implements SecurityContext {

  private static final long serialVersionUID = -3209120471953147538L;

  private Authentication authentication;

  public DefaultSecurityContext(Authentication authentication) {
    this.authentication = authentication;
  }

  public final Authentication getAuthentication() {
    return authentication;
  }

  public final void setAuthentication(Authentication authentication) {
    this.authentication = authentication;
  }
}
