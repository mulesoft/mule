/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;

/**
 * <code>SecurityContextFactory</code> is responsible for creating a SecurityContext instance. The factory itself is associated
 * with an Authentication class type on the SecurityManager
 */

public interface SecurityContextFactory {

  SecurityContext create(Authentication authentication);
}
