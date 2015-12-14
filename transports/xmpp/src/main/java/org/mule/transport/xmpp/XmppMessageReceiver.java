/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractReceiverWorker;

import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

/**
 * <code>XmppMessageReceiver</code> is responsible for receiving Mule events over XMPP.
 */
public class XmppMessageReceiver extends AbstractMessageReceiver implements PacketListener
{
    private XmppConversation xmppConversation;

    public XmppMessageReceiver(AbstractConnector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        XmppConnector xmppConnector = (XmppConnector) connector;
        xmppConversation = xmppConnector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        xmppConversation.connect(false);
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        xmppConversation.disconnect();
    }

    @Override
    protected void doStart() throws MuleException
    {
        xmppConversation.addPacketListener(this);
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        xmppConversation.removePacketListener(this);
    }

    @Override
    protected void doDispose()
    {
        xmppConversation = null;
    }

    protected Work createWork(Packet packet)
    {
        List<Object> list = new ArrayList<Object>();
        list.add(packet);

        return new XMPPWorker(list, this);
    }

    /**
     * @see org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
     */
    @Override
    public void processPacket(Packet packet)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("processing packet: " + packet.toXML());
        }

        try
        {
            Work work = createWork(packet);
            getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, connector);
        }
        catch (WorkException e)
        {
            logger.error("XMPP message receiver work failed: " + e.getMessage(), e);
        }
    }

    private class XMPPWorker extends AbstractReceiverWorker
    {
        public XMPPWorker(List<Object> packets, AbstractMessageReceiver receiver)
        {
            super(packets, receiver);
        }

        @Override
        protected void bindTransaction(Transaction tx) throws TransactionException
        {
            // XMPP does not support transactions
        }
    }
}
