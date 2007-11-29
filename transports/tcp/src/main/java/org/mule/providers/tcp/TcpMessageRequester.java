/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageRequester;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Request transformed Mule events from TCP.
 */
public class TcpMessageRequester extends AbstractMessageRequester
{

    private final TcpConnector connector;

    public TcpMessageRequester(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (TcpConnector) endpoint.getConnector();
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doRequest(long timeout) throws Exception
    {
        if (timeout > Integer.MAX_VALUE || timeout < 0)
        {
            throw new IllegalArgumentException("Timeout incorrect: " + timeout);
        }
        Socket socket = connector.getSocket(endpoint);
        try
        {
            Object result = TcpMessageDispatcher.receiveFromSocket(socket, (int)timeout, endpoint);
            if (result == null)
            {
                return null;
            }
            return new MuleMessage(connector.getMessageAdapter(result));
        }
        catch (SocketTimeoutException e)
        {
            // we don't necesarily expect to receive a resonse here
            if (logger.isDebugEnabled())
            {
                logger.debug("Socket timed out normally while doing a synchronous receive on endpointUri: "
                    + endpoint.getEndpointURI());
            }
            return null;
        }

    }

    protected synchronized void doDispose()
    {
        try
        {
            doDisconnect();
        }
        catch (Exception e)
        {
            logger.error("Failed to shutdown the dispatcher.", e);
        }
    }

    protected void doConnect() throws Exception
    {
        // Test the connection
        if (connector.isValidateConnections())
        {
            Socket socket = connector.getSocket(endpoint);
            connector.releaseSocket(socket, endpoint);
        }
    }

    protected void doDisconnect() throws Exception
    {
        //nothing to do
    }

}