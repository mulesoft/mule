/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.transport.AbstractMessageRequester;

import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Request transformed Mule events from TCP.
 */
public class TcpMessageRequester extends AbstractMessageRequester
{

    private final TcpConnector connector;

    public TcpMessageRequester(InboundEndpoint endpoint)
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
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
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
            return createMuleMessage(result, endpoint.getEncoding());
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
