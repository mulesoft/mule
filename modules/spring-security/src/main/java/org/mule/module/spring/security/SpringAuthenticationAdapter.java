/*
 * $Id: AcegiAuthenticationAdapter.java 10662 2008-02-01 13:10:14Z romikk $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.mule.api.security.Authentication;

import java.util.Map;

/**
 * 
 */
public class SpringAuthenticationAdapter implements Authentication
{
    private static final long serialVersionUID = -5906282218126929871L;

    private org.springframework.security.Authentication delegate;
    private Map properties;

    public SpringAuthenticationAdapter(org.springframework.security.Authentication authentication)
    {
        this.delegate = authentication;
    }

    public SpringAuthenticationAdapter(org.springframework.security.Authentication authentication, Map properties)
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

    public org.springframework.security.GrantedAuthority[] getAuthorities()
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

    public org.springframework.security.Authentication getDelegate()
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
