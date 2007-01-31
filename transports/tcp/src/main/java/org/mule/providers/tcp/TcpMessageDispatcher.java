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
import org.mule.transformers.simple.SerializableToByteArray;
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
    protected final SerializableToByteArray serializableToByteArray;

    public TcpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (TcpConnector)endpoint.getConnector();
        serializableToByteArray = new SerializableToByteArray();
    }



    protected synchronized void doDispatch(UMOEvent event) throws Exception
    {
        Socket socket = dispatchToSocket(event);
        connector.releaseSocket(socket, event.getEndpoint());
    }

    protected synchronized UMOMessage doSend(UMOEvent event) throws Exception
    {
        Socket socket = dispatchToSocket(event);

        try
        {
            if (useRemoteSync(event))
            {
                try
                {
                    Object result = receive(socket, event.getTimeout());
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

    /**
     * The doSend() and doDispatch() methods need to handle socket disposure
     * differently, thus the need to extract this common code.
     * 
     * @param event event
     * @throws Exception in case of any error
     */
    protected Socket dispatchToSocket(UMOEvent event) throws Exception
    {
        Object payload = event.getTransformedMessage();

        Socket socket;
        try
        {

            socket = connector.getSocket(event.getEndpoint());
            write(socket, payload);
            return socket;
            // If we're doing sync receive try and read return info from socket
        }
        catch (IOException e)
        {
            //TODO: Reconnects should be made posible gererically, by detecting an error in the AbstractDispatcher
            //and dispatching the event again. It seems wrong to put it here
//            if (connector.isKeepSendSocketOpen())
//            {
//                logger.warn("Write raised exception: '" + e.getMessage() + "' attempting to reconnect.");
//                // Try reconnecting or a Fatal Connection Exception will be thrown
//                connector.releaseSocket(socket, event.getEndpoint());
//                reconnect();
//                socket = connector.getSocket(event.getEndpoint());
//                write(socket, payload);
//            }
//            else
//            {
                throw e;
//            }
        }
    }

    

    protected void write(Socket socket, Object data) throws IOException, TransformerException
    {
        TcpProtocol protocol = connector.getTcpProtocol();

        byte[] binaryData;
        if (data instanceof String)
        {
            binaryData = data.toString().getBytes();
        }
        else if (data instanceof byte[])
        {
            binaryData = (byte[])data;
        }
        else
        {
            binaryData = (byte[])serializableToByteArray.transform(data);
        }

        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        protocol.write(bos, binaryData);
        bos.flush();
    }

    protected Object receive(Socket socket, int timeout) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        if (timeout >= 0)
        {
            socket.setSoTimeout(timeout);
        }
        // TODO SF: check if this cast is ok!
        return (byte[])connector.getTcpProtocol().read(dis);
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
        Socket socket = null;
        try
        {
            socket = connector.getSocket(endpoint);
            try
            {
                Object result = receive(socket, (int)timeout);
                if (result == null)
                {
                    return null;
                }
                return new MuleMessage(connector.getMessageAdapter(result));
            }
            catch (SocketTimeoutException e)
            {
                // we don't necesarily expect to receive a resonse here
                if(logger.isDebugEnabled())
                {
                    logger.debug("Socket timed out normally while doing a synchronous receive on endpointUri: "
                            + endpoint.getEndpointURI());
                }
                return null;
            }
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
        //Just test that we can connect. If we are keeping send sockets open, this will get cached
        Socket socket = connector.getSocket(endpoint);
        connector.releaseSocket(socket, endpoint);
    }

    protected void doDisconnect() throws Exception
    {
        Socket socket = connector.lookupSocket(endpoint);

        if (null != socket && !socket.isClosed())
        {
            try
            {
                socket.close();
            }
            catch (IOException e)
            {
                logger.warn("ConnectedSocked.close() raised exception. Reason: " + e.getMessage());
            }
            finally
            {
                connector.releaseSocket(socket, endpoint);
            }
        }
    }
}
