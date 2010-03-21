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

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.ConnectException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
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

    public void connect() throws ConnectException
    {
        doConnect();
        packetCollector = createPacketCollector();
    }
    
    /**
     * Subclasses can override this method to create their conversation specific connection.
     */
    protected void doConnect() throws ConnectException
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
    
    public void disconnect()
    {
        if (packetCollector != null)
        {
            packetCollector.cancel();
        }
        
        doDisconnect();
    }

    /**
     * Subclasses can override this method to perform custom disconnect actions.
     */
    protected void doDisconnect()
    {
        // template method
    }
    
    public Message receive(long timeout)
    {
        // The filter of our packetCollector should make sure that we receive only
        // Message instances here
        return (Message) packetCollector.nextResult(timeout);
    }
    
    public Message receive()
    {
        // The filter of our packetCollector should make sure that we receive only
        // Message instances here
        return (Message) packetCollector.nextResult();
    }
}
