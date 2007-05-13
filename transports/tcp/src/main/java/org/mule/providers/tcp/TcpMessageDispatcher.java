/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * <code>TcpMessageDispatcher</code> will send transformed Mule events over TCP.
 */

public class TcpMessageDispatcher extends AbstractMessageDispatcher
{
    private final TcpConnector connector;

    public TcpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (TcpConnector) endpoint.getConnector();
    }

    protected synchronized void doDispatch(UMOEvent event) throws Exception
    {
        Socket socket = connector.getSocket(event.getEndpoint());
        try 
        {
            dispatchToSocket(socket, event);
        }
        finally 
        {
            connector.releaseSocket(socket, event.getEndpoint());
        }
    }

    protected synchronized UMOMessage doSend(UMOEvent event) throws Exception
    {
        Socket socket = connector.getSocket(event.getEndpoint());
        try
        {
            dispatchToSocket(socket, event);

            if (useRemoteSync(event))
            {
                try
                {
                    Object result = receiveFromSocket(socket, event.getTimeout());
                    if (result == null)
                    {
                        return null;
                    }
                    return new MuleMessage(connector.getMessageAdapter(result));
                }
                catch (SocketTimeoutException e)
                {
                    // we don't necessarily expect to receive a response here
                    logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                        + event.getEndpoint().getEndpointURI());
                    return null;
                }
            }
            else
            {
                return event.getMessage();
            }
        }
        finally
        {
            connector.releaseSocket(socket, event.getEndpoint());
        }
    }

    // Socket management (get and release) is handled outside this method
    private void dispatchToSocket(Socket socket, UMOEvent event) throws Exception
    {
        Object payload = event.getTransformedMessage();
        write(socket, payload);
    }

    private void write(Socket socket, Object data) throws IOException, TransformerException
    {
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        connector.getTcpProtocol().write(bos, data);
        bos.flush();
    }

    private Object receiveFromSocket(Socket socket, int timeout) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        if (timeout >= 0)
        {
            socket.setSoTimeout(timeout);
        }
        return connector.getTcpProtocol().read(dis);
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        Socket socket = connector.getSocket(endpoint);
        try
        {
            Object result = receiveFromSocket(socket, (int)timeout);
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
        finally
        {
            connector.releaseSocket(socket, endpoint);
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
