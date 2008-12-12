/*
 * $Id: AcegiProviderAdapter.java 13181 2008-10-30 13:21:11Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security provider to
 * use with the SecurityManager
 */
public class SpringProviderAdapter extends AbstractSecurityProvider implements AuthenticationProvider
{
    private AuthenticationManager delegate;
    private Map securityProperties;

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

    protected void doInitialise() throws InitialisationException
    {
        setSecurityContextFactory(new SpringSecurityContextFactory());
    }

    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        org.springframework.security.Authentication auth = null;
        if (authentication instanceof SpringAuthenticationAdapter)
        {
            auth = ((SpringAuthenticationAdapter)authentication).getDelegate();
        }
        else
        {
            auth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                authentication.getCredentials());

        }
        auth = delegate.authenticate(auth);
        return new SpringAuthenticationAdapter(auth, getSecurityProperties());
    }

    public org.springframework.security.Authentication authenticate(org.springframework.security.Authentication authentication) throws AuthenticationException
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
}
