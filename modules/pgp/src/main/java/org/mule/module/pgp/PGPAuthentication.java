/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.MuleEvent;
import org.mule.api.security.Authentication;

import java.util.Map;

import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPAuthentication implements Authentication
{
    private boolean authenticated;
    private String userName;
    private Message message;
    private PGPPublicKey publicKey;
    transient private MuleEvent event;
    
    public PGPAuthentication(String userName, Message message)
    {
        this(userName, message, null);
    }

    public PGPAuthentication(String userName, Message message, MuleEvent event)
    {
        this.authenticated = false;
        this.userName = userName;
        this.message = message;
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
        return message;
    }

    public Object getDetails()
    {
        return publicKey;
    }

    protected void setDetails(PGPPublicKey publicKey)
    {
        this.publicKey = publicKey;
    }

    @Override
    public Object getPrincipal()
    {
        return userName;
    }

    @Override
    public Map<String, Object> getProperties()
    {
        // TODO
        return null;
    }

    @Override
    public void setProperties(Map<String, Object> properties)
    {
        // TODO
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
