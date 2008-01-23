/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;

import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;

import java.util.Map;

public class DefaultMuleAuthentication implements Authentication
{
    private boolean authenticated;
    private char[] credentials;
    private String user;
    private Map properties;

    public DefaultMuleAuthentication(Credentials credentials)
    {
        this.user = credentials.getUsername();
        this.credentials = credentials.getPassword();
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

}
