/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
