/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Send transformed Mule events over TCP.
 */
public class TcpMessageDispatcher extends AbstractMessageDispatcher
{

    private final TcpConnector connector;

    public TcpMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (TcpConnector) endpoint.getConnector();
    }

    @Override
    protected synchronized void doDispatch(MuleEvent event) throws Exception
    {
        Socket socket = connector.getSocket(endpoint);
        try 
        {
            dispatchToSocket(socket, event);
        }
        finally 
        {
            connector.releaseSocket(socket, endpoint);
        }
    }

    private void doDispatchToSocket(Socket socket, MuleEvent event) throws Exception
    {
        try
        {
            dispatchToSocket(socket, event);
        }
        catch(Exception e)
        {
            connector.releaseSocket(socket, endpoint);
            throw new Exception(e);
        }
    }

    @Override
    protected synchronized MuleMessage doSend(MuleEvent event) throws Exception
    {
        Socket socket = connector.getSocket(endpoint);
        doDispatchToSocket(socket, event);
        try
        {
            if (returnResponse(event))
            {
                try
                {
                    Object result = receiveFromSocket(socket, event.getTimeout(), endpoint);
                    if (result == null)
                    {
                        return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
                    }
                    
                    if (result instanceof MuleMessage)
                    {
                        return (MuleMessage) result;
                    }
                    
                    return createMuleMessage(result, endpoint.getEncoding());
                }
                catch (SocketTimeoutException e)
                {
                    // we don't necessarily expect to receive a response here
                    logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                        + endpoint.getEndpointURI());
                    return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
                }
            }
            else
            {
                return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
            }
        }
        finally
        {
            if (!returnResponse(event))
            {
                connector.releaseSocket(socket, endpoint);
            }
        }
        
    }

    // Socket management (get and release) is handled outside this method
    private void dispatchToSocket(Socket socket, MuleEvent event) throws Exception
    {
        Object payload = event.getMessage().getPayload();
        write(socket, payload);
    }

    private void write(Socket socket, Object data) throws IOException, TransformerException
    {
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        connector.getTcpProtocol().write(bos, data);
        bos.flush();
    }

    protected static Object receiveFromSocket(final Socket socket, int timeout, final ImmutableEndpoint endpoint)
            throws IOException
    {
        final TcpConnector connector = (TcpConnector) endpoint.getConnector();
        DataInputStream underlyingIs = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        TcpInputStream tis = new TcpInputStream(underlyingIs)
        {
            @Override
            public void close() throws IOException
            {
                try
                {
                    connector.releaseSocket(socket, endpoint);
                }
                catch (IOException e)
                {
                   throw e;
                }
                catch (Exception e)
                {
                    IOException e2 = new IOException();
                    e2.initCause(e);
                    throw e2;
                }
            }

        };

        int soTimeout = endpoint.getResponseTimeout() != 0 ? endpoint.getResponseTimeout() : timeout;
        if (soTimeout >= 0)
        {
            socket.setSoTimeout(soTimeout);
        }

        try
        {
            return connector.getTcpProtocol().read(tis);
        }
        finally
        {
            if (!tis.isStreaming())
            {
                tis.close();
            }
        }
    }

    @Override
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

    @Override
    protected void doConnect() throws Exception
    {
        // nothing, there is an optional validation in validateConnection()
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        //nothing to do
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        Socket socket = null;
        try
        {
            socket = connector.getSocket(endpoint);

            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    connector.releaseSocket(socket, endpoint);
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Failed to release a socket " + socket, e);
                    }
                }
            }
        }
        
        return retryContext;
    }
}
