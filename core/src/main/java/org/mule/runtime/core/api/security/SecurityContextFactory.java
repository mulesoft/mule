/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
