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
    private String serverName = null;
    
    private String username = null;
    
    private String password = null;

    public String getProtocol()
    {
    	return "xmpp";
    }

    /**
     * @return Returns the serverName.
     */
    public String getServerName()
    {
        return serverName;
    }
    /**
     * @param serverName The serverName to set.
     */
    public void setServerName(String serverName)
    {
        this.serverName = serverName;
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
