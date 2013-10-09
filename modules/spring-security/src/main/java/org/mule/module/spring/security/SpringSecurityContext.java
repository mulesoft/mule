/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

import org.springframework.security.core.context.SecurityContextHolder;


/**
 * <code>SpringSecurityContext</code> is a SecurityContext wrapper used to
 * interface with an Spring's {@link org.springframework.security.core.context.SecurityContext}.
 */

public class SpringSecurityContext implements SecurityContext
{
    private org.springframework.security.core.context.SecurityContext delegate;
    private SpringAuthenticationAdapter authentication;

    public SpringSecurityContext(org.springframework.security.core.context.SecurityContext delegate)
    
    {
        this.delegate = delegate;
        SecurityContextHolder.setContext(this.delegate);
    }

    public void setAuthentication(Authentication authentication)
    {
        this.authentication = ((SpringAuthenticationAdapter)authentication);
        delegate.setAuthentication(this.authentication.getDelegate());
        SecurityContextHolder.setContext(delegate);
    }

    public Authentication getAuthentication()
    {
        return this.authentication;
    }
}
