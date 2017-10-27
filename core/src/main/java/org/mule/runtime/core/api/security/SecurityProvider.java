/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;

/**
 * {@code SecurityProvider} is a target security provider that actually does the work of authenticating credentials and populating
 * the Authentication object.
 *
 * @since 4.0
 */
public interface SecurityProvider extends NameableObject {

  /**
   * Performs the authentication of a security request based either on the security realm configured in the {@link Authentication}
   * request or the default realm for the current context
   *
   * @param authentication The {@link Authentication} request
   * @return The authenticated response
   * @throws SecurityException In case authentication fails
   * @see SecurityProvider#authenticate(Authentication)
   */
  Authentication authenticate(Authentication authentication) throws SecurityException;

  /**
   * Checks if the class of authentication is supported by this security provider
   *
   * @param aClass The class to verify
   * @return Whether this class is supported
   */
  boolean supports(Class<?> aClass);

  /**
   * Creates the security context for this security provider
   *
   * @param auth The {@link Authentication} object
   * @return The {@link SecurityContext} for this provider
   * @throws UnknownAuthenticationTypeException In case the authentication type is not known.
   * @see SecurityProvider#createSecurityContext(Authentication)
   */
  SecurityContext createSecurityContext(Authentication auth) throws UnknownAuthenticationTypeException;
}
