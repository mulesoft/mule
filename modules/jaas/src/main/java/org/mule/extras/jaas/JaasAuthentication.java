/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOCredentials;

import java.util.Map;

import javax.security.auth.Subject;

public class JaasAuthentication implements UMOAuthentication
{
    private boolean authenticated;
    private char[] credentials;
    private String user;
    private Map properties;
    private Subject subject;
  
    public JaasAuthentication(UMOCredentials credentials)
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
