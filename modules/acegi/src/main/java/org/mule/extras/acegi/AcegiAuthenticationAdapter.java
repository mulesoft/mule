/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.api.security.Authentication;

import java.util.Map;

import org.acegisecurity.GrantedAuthority;

/**
 * <code>AcegiAuthenticationAdapter</code> TODO
 */
public class AcegiAuthenticationAdapter implements Authentication
{
    private org.acegisecurity.Authentication delegate;
    private Map properties;

    public AcegiAuthenticationAdapter(org.acegisecurity.Authentication authentication)
    {
        this.delegate = authentication;
    }

    public AcegiAuthenticationAdapter(org.acegisecurity.Authentication authentication, Map properties)
    {
        this.delegate = authentication;
        this.properties = properties;
    }

    public void setAuthenticated(boolean b)
    {
        delegate.setAuthenticated(b);
    }

    public boolean isAuthenticated()
    {
        return delegate.isAuthenticated();
    }

    public GrantedAuthority[] getAuthorities()
    {
        return delegate.getAuthorities();
    }

    public Object getCredentials()
    {
        return delegate.getCredentials();
    }

    public Object getDetails()
    {
        return delegate.getDetails();
    }

    public Object getPrincipal()
    {
        return delegate.getPrincipal();
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public boolean equals(Object another)
    {
        return delegate.equals(another);
    }

    public String getName()
    {
        return delegate.getName();
    }

    public org.acegisecurity.Authentication getDelegate()
    {
        return delegate;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }
}
