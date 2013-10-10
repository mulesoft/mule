/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    private Map<String, Object> properties;
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

    public Subject getSubject()
    {
        return subject;
    }
}
