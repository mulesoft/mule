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
package org.mule.providers.xmpp;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.endpoint.UMOEndpointURI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>XmppConnector</code> TODO
 * 
 * @author Peter Braswell
 * @author John Evans
 * @version $Revision$
 */
public class XmppConnector extends AbstractServiceEnabledConnector
{
    public static final String XMPP_PROPERTY_PREFIX = "";
    public static final String XMPP_SUBJECT = XMPP_PROPERTY_PREFIX + "subject";
    public static final String XMPP_THREAD = XMPP_PROPERTY_PREFIX + "thread";
    public static final String XMPP_TO = XMPP_PROPERTY_PREFIX + "to";
    public static final String XMPP_FROM = XMPP_PROPERTY_PREFIX + "from";
    public static final String XMPP_GROUP_CHAT = XMPP_PROPERTY_PREFIX + "groupChat";
    public static final String XMPP_NICKNAME = XMPP_PROPERTY_PREFIX + "nickname";

    private Map connCache = new HashMap();

    public String getProtocol()
    {
        return "xmpp";
    }

    public XMPPConnection findOrCreateXmppConnection(UMOEndpointURI endpointURI) throws XMPPException
    {
        logger.info("Trying to find XMPP connection for uri: " + endpointURI);
        XMPPConnection xmppConnection = null;

        String username = endpointURI.getUsername();
        String hostname = endpointURI.getHost();
        String password = endpointURI.getPassword();
        String resource = (String)endpointURI.getParams().get("resource");

        xmppConnection = findOrCreateConnection(endpointURI);

        logger.info("*************************************");
        logger.info("*            JABBER LOGIN           *");
        logger.info("*************************************");

        if (!xmppConnection.isAuthenticated()) {
            // Make sure we have an account. If we don't, make one.
            try {
                AccountManager accManager = new AccountManager(xmppConnection);
                accManager.createAccount(username, password);
            } catch (XMPPException ex) {
                // User probably already exists, throw away...
                logger.info("*** account (" + username + ") already exists ***");
            }

            logger.info("Logging in as: " + username);
            logger.info("pw is        : " + password);
            logger.info("server       : " + hostname);
            logger.info("resource     : " + resource);
            if(resource == null) {
                xmppConnection.login(username, password);
            } else {
                xmppConnection.login(username, password, resource);
            }
        } else {
            logger.info("Already authenticated on this connection, no need to log in again.");
        }

        logger.info("XMPP LOGIN COMPLETE!");

        return xmppConnection;
    }

    private XMPPConnection findOrCreateConnection(UMOEndpointURI uri) throws XMPPException
    {
        XMPPConnection conn = (XMPPConnection) connCache.get(uri);
        if (null == conn) {
            if(uri.getPort() != -1) {
                conn = new XMPPConnection(uri.getHost(), uri.getPort());
            } else {
                conn = new XMPPConnection(uri.getHost());
            }
            connCache.put(uri, conn);
        }
        return conn;
    }

    /**
     * Template method to perform any work when destroying the connectoe
     */
    protected void doDispose()
    {
        for (Iterator iterator = connCache.keySet().iterator(); iterator.hasNext();) {
            UMOEndpointURI uri = (UMOEndpointURI) iterator.next();
            XMPPConnection conn = (XMPPConnection) connCache.remove(uri);
            conn.close();
        }
    }

    public boolean isRemoteSyncEnabled() {
        return true;
    }
}
