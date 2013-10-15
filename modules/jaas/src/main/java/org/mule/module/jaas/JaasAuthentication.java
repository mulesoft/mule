/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import org.mule.api.MuleEvent;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;

import java.util.Map;

import javax.security.auth.Subject;

public class JaasAuthentication implements Authentication
{
    private boolean authenticated;
    private char[] credentials;
    private String user;
    private Map properties;
    private Subject subject;
    transient private MuleEvent event;
    
    public JaasAuthentication(Credentials credentials)
    {
        this.user = credentials.getUsername();
        this.credentials = credentials.getPassword();
    }
    
    public JaasAuthentication(Object user, Object credentials, Subject subject)
    {
        this.user = (String) user;
        this.credentials = ((String) credentials).toCharArray();
        this.subject = subject;
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
        authenticated = b;
    }

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public Object getCredentials()
    {
        return new String(credentials);
    }

    public Object getPrincipal()
    {
        return user;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    public Subject getSubject()
    {
        return subject;
    }
}
