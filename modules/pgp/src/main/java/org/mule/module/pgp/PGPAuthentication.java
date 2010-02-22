/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.api.security.Authentication;

import cryptix.message.Message;
import cryptix.pki.KeyBundle;

import java.util.Map;

public class PGPAuthentication implements Authentication
{
    boolean authenticated = false;
    private String userName;
    private Message message;
    private KeyBundle userKeyBundle = null;

    public PGPAuthentication(String userName, Message message)
    {
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
        return userKeyBundle;
    }

    protected void setDetails(KeyBundle kb)
    {
        userKeyBundle = kb;
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
