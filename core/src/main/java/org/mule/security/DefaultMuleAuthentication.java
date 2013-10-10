/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.MuleEvent;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;

import java.util.Map;

public class DefaultMuleAuthentication implements Authentication
{
    private boolean authenticated;
    private char[] credentials;
    private String user;
    private Map<String, Object> properties;
    transient private MuleEvent event;

    public DefaultMuleAuthentication(Credentials credentials)
    {
        this(credentials, null);
    }

    public DefaultMuleAuthentication(Credentials credentials, MuleEvent event)
    {
        this.event = event;
        this.user = credentials.getUsername();
        this.credentials = credentials.getPassword();
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

    @Override
    public void setAuthenticated(boolean b)
    {
        authenticated = b;
    }

    @Override
    public boolean isAuthenticated()
    {
        return authenticated;
    }

    @Override
    public Object getCredentials()
    {
        return new String(credentials);
    }

    @Override
    public Object getPrincipal()
    {
        return user;
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
}
