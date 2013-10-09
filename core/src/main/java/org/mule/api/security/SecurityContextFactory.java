/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

/**
 * <code>SecurityContextFactory</code> is responsible for creating a
 * SecurityContext instance. The factory itself is associated with an
 * Authentication class type on the SecurityManager
 */

public interface SecurityContextFactory
{
    SecurityContext create(Authentication authentication);
}
