/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.security;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOCredentials;

/**
 * <code>MuleAuthentication</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleAuthentication implements UMOAuthentication
{
    private boolean authenticated;
    private char[] credentials;
    private String user;
    private Object details;

    public MuleAuthentication(UMOCredentials credentials)
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

    public Object getDetails()
    {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public Object getPrincipal()
    {
        return user;
    }
}
