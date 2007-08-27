/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * <code>XmppConnector</code> TODO
 */
public class XmppConnector extends AbstractConnector
{
    public static final String XMPP_PROPERTY_PREFIX = "";
    public static final String XMPP_SUBJECT = XMPP_PROPERTY_PREFIX + "subject";
    public static final String XMPP_THREAD = XMPP_PROPERTY_PREFIX + "thread";
    public static final String XMPP_TO = XMPP_PROPERTY_PREFIX + "to";
    public static final String XMPP_FROM = XMPP_PROPERTY_PREFIX + "from";
    public static final String XMPP_GROUP_CHAT = XMPP_PROPERTY_PREFIX + "groupChat";
    public static final String XMPP_NICKNAME = XMPP_PROPERTY_PREFIX + "nickname";


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "xmpp";
    }

    public XMPPConnection createXmppConnection(UMOEndpointURI endpointURI) throws XMPPException
    {
        logger.info("Trying to find XMPP connection for uri: " + endpointURI);
        XMPPConnection xmppConnection = null;

        String username = endpointURI.getUsername();
        String hostname = endpointURI.getHost();
        String password = endpointURI.getPassword();
        String resource = (String)endpointURI.getParams().get("resource");

        if (endpointURI.getPort() != -1)
        {
            xmppConnection = new XMPPConnection(endpointURI.getHost(), endpointURI.getPort());
        }
        else
        {
            xmppConnection = new XMPPConnection(endpointURI.getHost());
        }

        if (!xmppConnection.isAuthenticated())
        {
            // Make sure we have an account. If we don't, make one.
            try
            {
                AccountManager accManager = new AccountManager(xmppConnection);
                accManager.createAccount(username, password);
            }
            catch (XMPPException ex)
            {
                // User probably already exists, throw away...
                logger.info("*** account (" + username + ") already exists ***");
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Logging in as: " + username);
                logger.debug("pw is        : " + password);
                logger.debug("server       : " + hostname);
                logger.debug("resource     : " + resource);
            }

            if (resource == null)
            {
                xmppConnection.login(username, password);
            }
            else
            {
                xmppConnection.login(username, password, resource);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
                logger.debug("Already authenticated on this connection, no need to log in again.");
        }
        return xmppConnection;
    }

    public boolean isRemoteSyncEnabled()
    {
        return true;
    }
}
