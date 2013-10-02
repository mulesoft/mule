/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.NameableObject;
import org.mule.api.lifecycle.Initialisable;

/**
 * <code>SecurityProvider</code> is a target security provider that actually does the work of authenticating
 * credentials and populating the Authentication object.
 */
public interface SecurityProvider extends Initialisable, NameableObject
{
    Authentication authenticate(Authentication authentication) throws SecurityException;

    boolean supports(Class<?> aClass);

    SecurityContext createSecurityContext(Authentication auth) throws UnknownAuthenticationTypeException;
}
