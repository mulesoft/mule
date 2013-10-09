/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

/**
 * Trivial {@link SecurityContext} implementation which simply holds the {@link Authentication} object.
 */
public class DefaultSecurityContext implements SecurityContext
{
    private static final long serialVersionUID = -3209120471953147538L;

    private Authentication authentication;

    public DefaultSecurityContext(Authentication authentication)
    {
        this.authentication = authentication;
    }

    public final Authentication getAuthentication()
    {
        return authentication;
    }

    public final void setAuthentication(Authentication authentication)
    {
        this.authentication = authentication;
    }
}
