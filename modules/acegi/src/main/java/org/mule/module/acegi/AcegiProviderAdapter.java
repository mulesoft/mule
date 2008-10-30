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
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;
import org.mule.security.AbstractSecurityProvider;

import java.util.Map;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security provider to
 * use with the SecurityManager
 */
public class AcegiProviderAdapter extends AbstractSecurityProvider implements AuthenticationProvider
{
    private AuthenticationProvider delegate;
    private Map securityProperties;

    /** For Spring IoC only */
    public AcegiProviderAdapter()
    {
        super();
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate)
    {
        this(delegate, "acegi");
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate, String name)
    {
        super(name);
        this.delegate = delegate;
    }

    protected void doInitialise() throws InitialisationException
    {
        setSecurityContextFactory(new AcegiSecurityContextFactory());
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

    public AuthenticationProvider getDelegate()
    {
        return delegate;
    }

    public void setDelegate(AuthenticationProvider delegate)
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
