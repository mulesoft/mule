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

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.config.i18n.Messages;

import java.net.URI;

/**
 * <code>XmppMessageReceiver</code> TODO
 *
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppMessageReceiver extends AbstractMessageReceiver implements
        PacketListener
{
    private XMPPConnection xmppConnection = null;

    private PooledExecutor threadPool;

    private Thread worker;

    public XmppMessageReceiver(AbstractConnector connector,
            UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        create(connector, component, endpoint);

        threadPool = connector.getReceiverThreadingProfile().createPool();
        connect(endpoint.getEndpointURI().getUri());
    }

    protected void connect(URI uri) throws InitialisationException
    {
        try
        {
            logger.info("*************************************");
            logger.info("*            JABBER LOGIN           *");
            logger.info("*************************************");

            XmppConnector cnn = (XmppConnector)connector;
            String serverName = cnn.getServerName();
            String userName = cnn.getUsername();
            String login = cnn.getPassword();
            
            // Make sure we have an account.  If we don't, make one.
            try 
            {
                xmppConnection = new XMPPConnection(serverName);
                AccountManager accManager = new AccountManager(xmppConnection);
                accManager.createAccount(userName, login);
            }
            catch (XMPPException ex) 
            {
                // User probably already exists, throw away...
                logger.info("*** mule outbound account already exists ***");
            }
            
            logger.info("Logging in as: "+userName);
            logger.info("pw is        : "+login);
            logger.info("server       : "+serverName);
            xmppConnection = new XMPPConnection(serverName);
            xmppConnection.login(userName, login);

            logger.info("JABBER LOGIN COMPLETE!");

            PacketFilter filter = new PacketTypeFilter(Message.class);
            PacketCollector myCollector = xmppConnection
                    .createPacketCollector(filter);
            xmppConnection.addPacketListener(this, filter);

        } catch (XMPPException e)
        {
            throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, "Xmpp Connection"), e, this);
        }
    }

    public void doDispose() throws UMOException
    {
        if (worker != null)
        {
            worker.interrupt();
            worker = null;
        }
        try
        {
            threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
            threadPool.awaitTerminationAfterShutdown();

        } catch (Exception e)
        {
            throw new DisposeException(e, this);
        }
        logger.info("Closed Xmpp connection");
    }

    protected Runnable createWorker(Message message)
    {
        return new XMPPWorker(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
     */
    public void processPacket(Packet arg0)
    {
        Message msg = (Message) arg0;
        Runnable worker = createWorker(msg);
        try
        {
            threadPool.execute(worker);
        } catch (InterruptedException e)
        {
            logger.error("Tcp Server receiver interrupted: " + e.getMessage(),
                    e);
        }
    }
    
    private class XMPPWorker implements Runnable
    {
        Message message = null;

        protected transient Log logger = LogFactory.getLog(XMPPWorker.class);

        public XMPPWorker(Message message)
        {
            this.message = message;
        }

        /**
         * Accept requests from a given TCP port
         */
        public void run()
        {
            try
            {
                logger.info("processing xmpp message from: "+message.getFrom());
                UMOMessageAdapter adapter = connector.getMessageAdapter(message);
                logger.info("UMOMessageAdapter is a: "
                        + adapter.getClass().getName());
                UMOMessage returnMessage = routeMessage(
                        new MuleMessage(adapter), endpoint.isSynchronous());

                if (returnMessage != null)
                {
                    xmppConnection.sendPacket(new Message(returnMessage.getPayloadAsString()));
                }
            } catch (Exception e)
            {
                handleException(e);
            }
        }
    }
}