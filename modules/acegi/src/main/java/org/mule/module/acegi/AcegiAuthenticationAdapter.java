/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.acegi;

import org.mule.api.MuleEvent;
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
    private MuleEvent event;
    
    public AcegiAuthenticationAdapter(org.acegisecurity.Authentication authentication)
    {
        this(authentication, null);
    }

    public AcegiAuthenticationAdapter(org.acegisecurity.Authentication authentication, Map properties)
    {
        this(authentication, properties, null);
    }

    public AcegiAuthenticationAdapter(org.acegisecurity.Authentication authentication, Map properties, MuleEvent event)
    {
        this.delegate = authentication;
        this.properties = properties;
        this.event = event;
    }

    public MuleEvent getEvent()
    {
        return event;
    }

    public void setEvent(MuleEvent muleEvent)
    {
        this.event = muleEvent;
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
