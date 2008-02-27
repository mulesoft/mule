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
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.transport.AbstractMessageRequester;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule events to be received over Xmpp
 */

public class XmppMessageRequester extends AbstractMessageRequester
{

    private final XmppConnector connector;
    private volatile XMPPConnection xmppConnection = null;

    public XmppMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XmppConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (xmppConnection == null)
        {
            EndpointURI uri = endpoint.getEndpointURI();
            xmppConnection = connector.createXmppConnection(uri);
        }
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            if (xmppConnection != null)
            {
                xmppConnection.close();
            }
        }
        finally
        {
            xmppConnection = null;
        }
    }

    protected void doDispose()
    {
        // template method
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        // Should be in the form of xmpp://user:pass@host:[port]/folder
        String to = (String)endpoint.getProperty("folder");
        if (to == null)
        {
            throw new MalformedEndpointException(endpoint.getEndpointURI().toString());
        }
        Chat chat = xmppConnection.createChat(to);
        Message message = null;
        if (timeout == MuleEvent.TIMEOUT_WAIT_FOREVER)
        {
            message = chat.nextMessage();
        }
        else if (timeout == MuleEvent.TIMEOUT_DO_NOT_WAIT)
        {
            message = chat.nextMessage(1);
        }
        else
        {
            message = chat.nextMessage(timeout);
        }
        if (message != null)
        {
            return new DefaultMuleMessage(connector.getMessageAdapter(message));
        }
        else
        {
            return null;
        }
    }

}