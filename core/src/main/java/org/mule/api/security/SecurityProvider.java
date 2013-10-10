/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    boolean supports(Class aClass);

    SecurityContext createSecurityContext(Authentication auth) throws UnknownAuthenticationTypeException;
}
