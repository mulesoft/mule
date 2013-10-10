/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.mule.api.MuleEvent;
import org.mule.api.security.Authentication;

import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

public class SpringAuthenticationAdapter implements Authentication
{
    private static final long serialVersionUID = -5906282218126929871L;

    private org.springframework.security.core.Authentication delegate;
    private Map<String, Object> properties;
    transient private MuleEvent event;
    
    public SpringAuthenticationAdapter(org.springframework.security.core.Authentication authentication)
    {
        this(authentication, null);
    }

    public SpringAuthenticationAdapter(org.springframework.security.core.Authentication authentication, Map<String, Object> properties)
    {
        this(authentication, properties, null);
    }

    public SpringAuthenticationAdapter(org.springframework.security.core.Authentication authentication, Map<String, Object> properties, MuleEvent event)
    {
        this.delegate = authentication;
        this.properties = properties;
        this.event = event;
    }

    @Override
    public void setAuthenticated(boolean b)
    {
        delegate.setAuthenticated(b);
    }

    @Override
    public boolean isAuthenticated()
    {
        return delegate.isAuthenticated();
    }

    public org.springframework.security.core.GrantedAuthority[] getAuthorities()
    {
        return delegate.getAuthorities().toArray(new GrantedAuthority[delegate.getAuthorities().size()]);
    }

    @Override
    public Object getCredentials()
    {
        return delegate.getCredentials();
    }

    public Object getDetails()
    {
        return delegate.getDetails();
    }

    @Override
    public Object getPrincipal()
    {
        return delegate.getPrincipal();
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object another)
    {
        return delegate.equals(another);
    }

    public String getName()
    {
        return delegate.getName();
    }

    public org.springframework.security.core.Authentication getDelegate()
    {
        return delegate;
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }

    @Override
    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    @Override
    public MuleEvent getEvent()
    {
        return event;
    }

    public void setEvent(MuleEvent muleEvent)
    {
        this.event = muleEvent;
    }
}
