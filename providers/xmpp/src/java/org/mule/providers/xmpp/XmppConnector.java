/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.xmpp;

import org.mule.providers.AbstractServiceEnabledConnector;

/**
 * <code>XmppConnector</code> TODO
 *
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppConnector extends AbstractServiceEnabledConnector
{
    private String hostname = null;
    
    private String username = null;
    
    private String password = null;

    public String getProtocol()
    {
    	return "xmpp";
    }

    /**
     * @return Returns the hostname.
     */
    public String getHostname()
    {
        return hostname;
    }
    /**
     * @param hostname The hostname to set.
     */
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }
    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }
    /**
     * @param password The password to set.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }
    /**
     * @return Returns the username.
     */
    public String getUsername()
    {
        return username;
    }
    /**
     * @param username The username to set.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }
}
