/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/** 
 * <code>XmppMessageReceiver</code> is responsible for receiving Mule events over XMPP. 
 */
public class XmppMessageReceiver extends AbstractMessageReceiver implements PacketListener
{
//    private XMPPConnection xmppConnection = null;
    private XmppConversation xmppConversation;

    public XmppMessageReceiver(AbstractConnector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);
        XmppConnector xmppConnector = (XmppConnector) connector;
        xmppConversation = xmppConnector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        xmppConversation.connect();
//        try
//        {
//            XmppConnector cnn = (XmppConnector) connector;
//            xmppConnection = cnn.createXmppConnection(endpoint.getEndpointURI());
//            if (endpoint.getFilter() instanceof PacketFilter)
//            {
//                xmppConnection.addPacketListener(this, (PacketFilter) endpoint.getFilter());
//            }
//            else
//            {
//                PacketFilter filter = new PacketTypeFilter(Message.class);
//                xmppConnection.addPacketListener(this, filter);
//            }
//        }
//        catch (XMPPException e)
//        {
//            throw new ConnectException(CoreMessages.failedToCreate("XMPP Connection"), e, this);
//        }
    }

    protected void doDisconnect() throws Exception
    {
        xmppConversation.disconnect();
//        if (xmppConnection != null)
//        {
//            xmppConnection.removePacketListener(this);
//            xmppConnection.disconnect();
//        }
    }

    // TODO xmpp: consider lifecycle
    protected void doStart() throws MuleException
    {
        // nothing to do
    }

    // TODO xmpp: consider lifecycle
    protected void doStop() throws MuleException
    {
        // nothing to do
    }

    protected void doDispose()
    {
        xmppConversation = null;
    }

    protected Work createWork(Packet message)
    {
        return new XMPPWorker(message);
    }

    /** @see org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet) */
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

        /** Accept requests from a given TCP port */
        public void run()
        {
            try
            {
                MessageAdapter adapter = connector.getMessageAdapter(packet);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Processing XMPP packet from: " + packet.getFrom());
                    logger.debug("MessageAdapter is a: " + adapter.getClass().getName());
                }

                MuleMessage returnMessage = routeMessage(new DefaultMuleMessage(adapter, connector.getMuleContext()), endpoint.isSynchronous());

                if (returnMessage != null && packet instanceof Message)
                {
                    returnMessage.applyTransformers(connector.getDefaultResponseTransformers());
                    Packet result = (Packet) returnMessage.getPayload();
//                    xmppConnection.sendPacket(result);
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
