/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;


public abstract class AbstractXmppConversation implements XmppConversation
{
    protected final Log logger = LogFactory.getLog(getClass());

    protected XMPPConnection connection;
    protected String recipient;
    protected PacketCollector packetCollector;

    public AbstractXmppConversation(ImmutableEndpoint endpoint)
    {
        super();
        connection = ((XmppConnector) endpoint.getConnector()).getXmppConnection();
        recipient = XmppConnector.getRecipient(endpoint);
    }

    @Override
    public void connect() throws NoResponseException, SmackException, XMPPException
    {
        doConnect();
        packetCollector = createPacketCollector();
    }

    /**
     * Subclasses can override this method to create their conversation specific connection.
     * @throws SmackException 
     * @throws NoResponseException 
     * @throws XMPPException 
     */
    protected void doConnect() throws NoResponseException, SmackException, XMPPException
    {
        // template method
    }

    /**
     * @return a {@link PacketCollector} that can be used to retrieve messages for this
     * conversation.
     */
    protected PacketCollector createPacketCollector()
    {
        PacketFilter filter = createPacketFilter();
        return connection.createPacketCollector(filter);
    }

    /**
     * @return a {@link PacketFilter} instance that matches the desired message type and recipient
     * for this conversation.
     */
    protected PacketFilter createPacketFilter()
    {
        return null;
    }

    @Override
    public void disconnect() throws NotConnectedException
    {
        if (packetCollector != null)
        {
            packetCollector.cancel();
        }

        doDisconnect();
    }

    /**
     * Subclasses can override this method to perform custom disconnect actions.
     * @throws NotConnectedException 
     */
    protected void doDisconnect() throws NotConnectedException
    {
        // template method
    }

    @Override
    public void addPacketListener(PacketListener listener)
    {
        PacketFilter filter = createPacketFilter();
        connection.addPacketListener(listener, filter);
    }

    @Override
    public void removePacketListener(PacketListener listener)
    {
        connection.removePacketListener(listener);
    }

    @Override
    public Message receive(long timeout)
    {
        // The filter of our packetCollector should make sure that we receive only
        // Message instances here
        return (Message) packetCollector.nextResult(timeout);
    }

    @Override
    public Message receive()
    {
        // The filter of our packetCollector should make sure that we receive only
        // Message instances here
        return (Message) packetCollector.nextResult();
    }
}
