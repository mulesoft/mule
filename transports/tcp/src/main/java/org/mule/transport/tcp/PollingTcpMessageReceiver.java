/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.tcp.i18n.TcpMessages;
import org.mule.util.MapUtils;

import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * <code>PollingTcpMessageReceiver</code> acts like a TCP client polling for new
 * messages.
 * 
 * @author esteban.robles
 */
public class PollingTcpMessageReceiver extends AbstractPollingMessageReceiver
{
    private int timeout;

    private PollingTcpConnector connector;

    public PollingTcpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);

        if (connector instanceof PollingTcpConnector)
        {
            this.connector = (PollingTcpConnector) connector;
        }
        else
        {
            throw new CreateException(TcpMessages.pollingReceiverCannotbeUsed(), this);
        }

        timeout = MapUtils.getIntValue(endpoint.getProperties(), "clientSoTimeout",
            this.connector.getClientSoTimeout());

        if (timeout > Integer.MAX_VALUE || timeout < 0)
        {
            throw new IllegalArgumentException("Timeout incorrect: " + timeout);
        }

        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
            this.connector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            this.setFrequency(pollingFrequency);
        }
    }

    @Override
    public void poll() throws Exception
    {
        Socket socket = connector.getSocket(endpoint);
        try
        {
            Object result = TcpMessageDispatcher.receiveFromSocket(socket, timeout, endpoint);
            if (!(result == null))
            {
                this.routeMessage(new DefaultMuleMessage(result, connector.getMuleContext()));
                if (logger.isDebugEnabled())
                {
                    logger.debug("Routing new message: " + result);
                }
            }
        }
        catch (SocketTimeoutException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Socket timed out normally while doing a synchronous receive on endpointUri: "
                             + endpoint.getEndpointURI());
            }
        }
        finally
        {
            connector.releaseSocket(socket, endpoint);
        }
    }
}
