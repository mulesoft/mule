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

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * <code>XmppMessageDispatcher</code> allows Mule events to be sent and recieved over Xmpp
 *
 * @author Peter Braswell
 * @version $Revision$
 */

public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory
            .getLog(XmppMessageDispatcher.class);

    private XmppConnector connector;

    private SynchronizedBoolean initialized = new SynchronizedBoolean(false);

    private XMPPConnection xmppConnection = null;

    private static int connectionCount = 0;

    public XmppMessageDispatcher(XmppConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    protected synchronized int getNextMuleId()
    {
        return connectionCount++;
    }

    protected synchronized void initialise(String endpoint) throws IOException, URISyntaxException
    {
        if (!initialized.get())
        {
            XmppConnector cnn = (XmppConnector) connector;
            String serverName = cnn.getServerName();
            String userName = cnn.getUsername();
            String login = cnn.getPassword();

            userName = "muleout" + new Integer(getNextMuleId()).toString();
            XMPPException xmppex = null;

            try
            {
                try
                {
                    xmppConnection = new XMPPConnection(serverName);
                    AccountManager accManager = new AccountManager(xmppConnection);
                    accManager.createAccount(userName, login);
                } catch (XMPPException ex)
                {
                    // User probably already exists, throw away...
                    logger.warn("*** mule outbound account already exists ***");
                    xmppex = ex;
                }
                logger.warn("logging in: uid=" + userName + " pw=" + login + "\n");
                if (!xmppConnection.isConnected())
                {
                    logger.warn("******************************************");
                    logger.warn("   FAILED TO LOG INTO SERVER!!!");
                    logger.warn("   previous exception was: " + xmppex.getMessage());
                    logger.warn("******************************************");
                }
                xmppConnection.login(userName, login);
                initialized.set(true);
            } catch (XMPPException e)
            {
                // TODO Auto-generated catch block
                logger.error("error initailizing: " + e.getMessage(), e);
            }

        }
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        logger.info("in doDispatch()");
        initialise(event.getEndpoint().getEndpointURI().getAddress());
        try
        {
            Message message = (Message) event.getTransformedMessage();
            while (!xmppConnection.isConnected() && !initialized.get())
            {
                initialise(null);
                Thread.sleep(150);
            }
            try
            {
                xmppConnection.createChat(message.getTo())
                        .sendMessage(message);
            } catch (IllegalStateException isex)
            {
                logger.fatal("************************************");
                logger.fatal("BIG PROBLEMS: ", isex);
                logger.fatal("************************************");
                System.exit(0);
            }
        } catch (ClassCastException ccex)
        {
            logger.error("something bad happened: ", ccex);
            logger.error("object was a :" + event.getTransformedMessage().getClass().getName());
        }
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
         doDispatch(event);
        return null;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose() throws UMOException
    {
        xmppConnection.close();
        initialized.set(false);
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        //todo
        throw new UnsupportedOperationException("xmpp receive not implemented yet");
    }
}
