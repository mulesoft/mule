/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;
import org.mule.security.AbstractSecurityProvider;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.AuthenticationException;


/**
 * <code>SpringProviderAdapter</code> is a wrapper for a Spring Security provider to
 * use with the SecurityManager
 */
public class SpringProviderAdapter extends AbstractSecurityProvider implements AuthenticationProvider
{
    private AuthenticationManager delegate;
    private Map securityProperties;
    private SpringAuthenticationProvider authenticationProvider;

    /** For Spring IoC only */
    public SpringProviderAdapter()
    {
        super("spring-security");
    }

    public SpringProviderAdapter(AuthenticationManager delegate)
    {
        this(delegate, "spring-security");
    }

    public SpringProviderAdapter(AuthenticationManager delegate, String name)
    {
        super(name);
        this.delegate = delegate;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        setSecurityContextFactory(new SpringSecurityContextFactory());
    }

    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        org.springframework.security.core.Authentication auth = null;        
        if (authentication instanceof SpringAuthenticationAdapter)
        {
            auth = ((SpringAuthenticationAdapter)authentication).getDelegate();
        }
        else
        {
            auth = this.getAuthenticationProvider().getAuthentication(authentication);

        }
        auth = delegate.authenticate(auth);
        return new SpringAuthenticationAdapter(auth, getSecurityProperties(), authentication.getEvent());
    }

    public org.springframework.security.core.Authentication authenticate(org.springframework.security.core.Authentication authentication) throws AuthenticationException    
    {
        return delegate.authenticate(authentication);
    }

    public AuthenticationManager getDelegate()
    {
        return delegate;
    }

    public void setDelegate(AuthenticationManager delegate)
    {
        this.delegate = delegate;
    }

    public Map getSecurityProperties()
    {
        return securityProperties;
    }

    public void setSecurityProperties(Map securityProperties)
    {
        this.securityProperties = securityProperties;
    }

    public SpringAuthenticationProvider getAuthenticationProvider()
    {
        if (this.authenticationProvider == null) {
            this.authenticationProvider = new UserAndPasswordAuthenticationProvider();
        }
        return authenticationProvider;
    }

    public void setAuthenticationProvider(SpringAuthenticationProvider authenticationProvider)
    {
        this.authenticationProvider = authenticationProvider;
    }
}
