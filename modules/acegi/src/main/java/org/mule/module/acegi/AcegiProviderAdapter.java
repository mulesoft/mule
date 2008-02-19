/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.acegi;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.UnknownAuthenticationTypeException;

import java.util.Map;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security provider to
 * use with the SecurityManager
 */
public class AcegiProviderAdapter implements SecurityProvider, AuthenticationProvider
{
    private AuthenticationProvider delegate;
    private String name;
    private SecurityContextFactory factory;
    private Map securityProperties;

    public AcegiProviderAdapter()
    {
        super();
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate)
    {
        this.delegate = delegate;
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate, String name)
    {
        this.delegate = delegate;
        this.name = name;
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        // all initialisation should be handled in the spring
        // intitialisation hook afterPropertiesSet()

        // register context factory
        factory = new AcegiSecurityContextFactory();
        return LifecycleTransitionResult.OK;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        org.acegisecurity.Authentication auth = null;
        if (authentication instanceof AcegiAuthenticationAdapter)
        {
            auth = ((AcegiAuthenticationAdapter)authentication).getDelegate();
        }
        else
        {
            auth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                authentication.getCredentials());

        }
        auth = delegate.authenticate(auth);
        return new AcegiAuthenticationAdapter(auth, getSecurityProperties());
    }

    public org.acegisecurity.Authentication authenticate(org.acegisecurity.Authentication authentication) throws AuthenticationException
    {
        return delegate.authenticate(authentication);
    }

    public boolean supports(Class aClass)
    {
        return Authentication.class.isAssignableFrom(aClass);
    }

    public AuthenticationProvider getDelegate()
    {
        return delegate;
    }

    public void setDelegate(AuthenticationProvider delegate)
    {
        this.delegate = delegate;
    }

    public SecurityContext createSecurityContext(Authentication auth)
        throws UnknownAuthenticationTypeException
    {
        /*
         * if (strategy != null){ return factory.create(auth, strategy); } else {
         */
        return factory.create(auth);
        // }
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
