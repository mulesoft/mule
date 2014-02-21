/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.NullPayload;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * <code>LocalSocketTcpMessageDispatcher</code> will send transformed Mule events
 * over TCP. It contains a local socket that reuses on each message dispatch
 * 
 * @since 2.2.6
 */
public class LocalSocketTcpMessageDispatcher extends TcpMessageDispatcher
{
    private AbstractTcpSocketFactory socketFactory;

    private Socket socket;

    public LocalSocketTcpMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.socketFactory = this.getConnector().getSocketFactory();
    }

    @Override
    public TcpConnector getConnector()
    {
        return (TcpConnector) super.getConnector();
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        dispatchToSocket(event);
    }

    @Override
    protected synchronized MuleMessage doSend(MuleEvent event) throws Exception
    {
        try
        {
            dispatchToSocket(event);
            if (returnResponse(event))
            {
                try
                {
                    Object result = receiveFromSocket(socket, event.getTimeout(), endpoint);
                    if (result == null)
                    {
                        return new DefaultMuleMessage(NullPayload.getInstance(), this.getEndpoint().getMuleContext());
                    }

                    if (result instanceof MuleMessage)
                    {
                        return (MuleMessage) result;
                    }

                    return new DefaultMuleMessage(result, this.getEndpoint()
                            .getMuleContext());
                }
                catch (Exception ex)
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Error occurred while Reading; Message: " + ex.getMessage(), ex);
                    }
                    closeSocket();
                    throw ex;
                }

            }
            else
            {
                return event.getMessage();
            }
        }
        finally
        {
            if (!this.getConnector().isKeepSendSocketOpen())
            {
                closeSocket();
            }
        }
    }

    private void closeSocket()
    {
        try
        {
            socket.close();
            socket = null;
        }
        catch (Exception ex)
        {
            logger.info("Error occurred while closing socket; Message: " + ex.getMessage());
        }
    }

    protected void dispatchToSocket(MuleEvent event) throws Exception
    {
        if (socket == null || socket.isClosed())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Socket is null; Creating... ");
            }
            TcpSocketKey socketKey = new TcpSocketKey(endpoint);
            socket = (Socket) socketFactory.makeObject(socketKey); // connector.getSocket(event.getEndpoint());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Is socket closed? " + (socket != null && socket.isClosed()));
        }
        try
        {
            Object payload = event.getMessage().getPayload();// getTransformedMessage();
            // following line was added set the payload in the threadlocal
            // so that a protocol class can use the thread local and pick the
            // transformed
            // message.
            event.getMessage().setPayload(payload);
            // OptimizedRequestContext.unsafeRewriteEvent(new DefaultMuleMessage(
            // payload));
            write(payload);
            return;
        }
        catch (IOException ioEx)
        {
            closeSocket();
            if (logger.isInfoEnabled())
            {
                logger.info("Error occurred while Writing; Message: " + ioEx.getMessage(), ioEx);
            }
            if (ioEx instanceof SocketTimeoutException)
            {
                throw ioEx;
            }
        }
        catch (Exception ex)
        {
            logger.info("Unknown Error occurred while Writing; Message: " + ex.getMessage(), ex);
        }
    }

    private void write(Object data) throws IOException, TransformerException
    {
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        this.getConnector().getTcpProtocol().write(bos, data);
        bos.flush();
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        try
        {
            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        return retryContext;
    }
}
