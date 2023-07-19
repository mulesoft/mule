/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContextFactory;
import org.mule.runtime.api.security.SecurityContext;

public class DefaultSecurityContextFactory implements SecurityContextFactory {

  public final SecurityContext create(Authentication authentication) {
    return new DefaultSecurityContext(authentication);
  }
}
