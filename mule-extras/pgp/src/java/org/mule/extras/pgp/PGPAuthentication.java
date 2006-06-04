/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pgp;

import cryptix.message.Message;
import cryptix.pki.KeyBundle;

import org.mule.umo.security.UMOAuthentication;

/**
 * @author ariva
 * 
 */
public class PGPAuthentication implements UMOAuthentication
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOAuthentication#setAuthenticated(boolean)
     */
    public void setAuthenticated(boolean b)
    {
        authenticated = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOAuthentication#isAuthenticated()
     */
    public boolean isAuthenticated()
    {
        return authenticated;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOAuthentication#getCredentials()
     */
    public Object getCredentials()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOAuthentication#getDetails()
     */
    public Object getDetails()
    {
        return userKeyBundle;
    }

    protected void setDetails(KeyBundle kb)
    {
        userKeyBundle = kb;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOAuthentication#getPrincipal()
     */
    public Object getPrincipal()
    {
        return userName;
    }

}
