/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnknownAuthenticationTypeException;

import java.util.Map;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security provider to
 * use with the UMOSecurityManager
 */
public class AcegiProviderAdapter implements UMOSecurityProvider, AuthenticationProvider
{
    private AuthenticationProvider delegate;
    private String name;
    private UMOSecurityContextFactory factory;
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

    public void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        // //all initialisation should be handled in the spring
        // intitialisation hook afterPropertiesSet()

        // register context factory
        factory = new AcegiSecurityContextFactory();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws SecurityException
    {
        Authentication auth = null;
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

    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        return delegate.authenticate(authentication);
    }

    public boolean supports(Class aClass)
    {
        return UMOAuthentication.class.isAssignableFrom(aClass);
    }

    public AuthenticationProvider getDelegate()
    {
        return delegate;
    }

    public void setDelegate(AuthenticationProvider delegate)
    {
        this.delegate = delegate;
    }

    public UMOSecurityContext createSecurityContext(UMOAuthentication auth)
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
