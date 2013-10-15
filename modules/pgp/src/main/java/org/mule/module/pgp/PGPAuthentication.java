/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.mule.api.security.Authentication;

import java.util.Map;

import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPAuthentication implements Authentication
{
    private boolean authenticated;
    private String userName;
    private Message message;
    private PGPPublicKey publicKey;

    public PGPAuthentication(String userName, Message message)
    {
        this.authenticated = false;
        this.userName = userName;
        this.message = message;
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

    public Object getPrincipal()
    {
        return userName;
    }

    public Map getProperties()
    {
        // TODO
        return null;
    }

    public void setProperties(Map securityMode)
    {
        // TODO

    }
}
