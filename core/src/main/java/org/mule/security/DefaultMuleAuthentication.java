/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
