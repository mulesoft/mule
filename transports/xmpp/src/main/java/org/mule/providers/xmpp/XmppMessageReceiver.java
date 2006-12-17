/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

/**
 * <code>XmppMessageReceiver</code> is responsible for receiving Mule events over XMPP.
 */
public class XmppMessageReceiver extends AbstractMessageReceiver implements PacketListener
{
    private XMPPConnection xmppConnection = null;

    public XmppMessageReceiver(AbstractConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    public void doConnect() throws Exception
    {
        try
        {
            XmppConnector cnn = (XmppConnector)connector;
            xmppConnection = cnn.createXmppConnection(endpoint.getEndpointURI());
            if (endpoint.getFilter() instanceof PacketFilter)
            {
                xmppConnection.addPacketListener(this, (PacketFilter)endpoint.getFilter());
            }
            else
            {
                PacketFilter filter = new PacketTypeFilter(Message.class);
                xmppConnection.addPacketListener(this, filter);
            }
        }
        catch (XMPPException e)
        {
            throw new ConnectException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X,
                "XMPP Connection"), e, this);
        }
    }

    public void doDisconnect() throws Exception
    {
        if (xmppConnection != null)
        {
            xmppConnection.removePacketListener(this);
            xmppConnection.close();
        }
    }

    protected void doDispose()
    {
        logger.info("Closed Xmpp Listener");
    }

    protected Work createWork(Packet message)
    {
        return new XMPPWorker(message);
    }

    /**
     * @see org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
     */
    public void processPacket(Packet packet)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("processing packet: " + packet.toXML());
        }

        Work work = createWork(packet);
        try
        {
            getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, connector);
        }
        catch (WorkException e)
        {
            logger.error("Xmpp Server receiver work failed: " + e.getMessage(), e);
        }
    }

    private class XMPPWorker implements Work
    {
        Packet packet = null;

        public XMPPWorker(Packet message)
        {
            this.packet = message;
        }

        /**
         * Accept requests from a given TCP port
         */
        public void run()
        {
            try
            {
                UMOMessageAdapter adapter = connector.getMessageAdapter(packet);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Processing XMPP packet from: " + packet.getFrom());
                    logger.debug("UMOMessageAdapter is a: " + adapter.getClass().getName());
                }

                UMOMessage returnMessage = routeMessage(new MuleMessage(adapter), endpoint.isSynchronous());

                if (returnMessage != null && packet instanceof Message)
                {
                    RequestContext.rewriteEvent(returnMessage);
                    Packet result = (Packet)connector.getDefaultResponseTransformer().transform(
                        returnMessage.getPayload());
                    xmppConnection.sendPacket(result);
                }
            }
            catch (Exception e)
            {
                handleException(e);
            }
        }

        public void release()
        {
            // template method
        }
    }
}
