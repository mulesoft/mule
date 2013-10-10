/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jca;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.ReceiveException;
import org.mule.module.client.i18n.ClientMessages;
import org.mule.module.jca.i18n.JcaMessages;
import org.mule.security.MuleCredentials;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.AbstractConnector;

import java.util.Map;

import javax.resource.ResourceException;

/**
 * <code>MuleConnection</code> TODO
 */
public class DefaultMuleConnection implements MuleConnection
{
    private final MuleCredentials credentials;
    private final MuleContext muleContext;
    private MuleManagedConnection managedConnection;

    public DefaultMuleConnection(MuleManagedConnection managedConnection,
                                 MuleContext muleContext,
                                 MuleCredentials credentials)
    {
        this.muleContext = muleContext;
        this.credentials = credentials;
        this.managedConnection = managedConnection;
    }

    /**
     * Dispatches an event asynchronously to a endpointUri via a mule server. the Url
     * determines where to dispathc the event to, this can be in the form of
     *
     * @param url the Mule url used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @throws org.mule.api.MuleException
     */
    public void dispatch(String url, Object payload, Map messageProperties) throws MuleException
    {
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        OutboundEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(url);
        MuleEvent event = getEvent(message,endpoint);
        try
        {
            endpoint.process(event);
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DispatchException(ClientMessages.failedToDispatchClientEvent(), event,
                endpoint, e);
        }
    }

    /**
     * Sends an object (payload) synchronous to the given url and returns a
     * MuleMessage response back.
     *
     * @param url the Mule url used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @return a response.
     * @throws org.mule.api.MuleException
     */
    public MuleMessage send(String url, Object payload, Map messageProperties) throws MuleException
    {
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        OutboundEndpoint endpoint = getOutboundEndpoint(url, MessageExchangePattern.REQUEST_RESPONSE);
        MuleEvent event = getEvent(message, endpoint);

        try
        {
            MuleEvent resultEvent = endpoint.process(event);
            if (resultEvent != null)
            {
                return resultEvent.getMessage();
            }
            else
            {
                return null;
            }
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DispatchException(ClientMessages.failedToDispatchClientEvent(), event,
                endpoint, e);
        }
    }

    /**
     * Will receive an event from an endpointUri determined by the url
     *
     * @param url the Mule url used to determine the destination and transport of the
     *            message
     * @param timeout how long to block waiting to receive the event, if set to 0 the
     *            receive will not wait at all and if set to -1 the receive will wait
     *            forever
     * @return the message received or null if no message was received
     * @throws org.mule.api.MuleException
     */
    public MuleMessage request(String url, long timeout) throws MuleException
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(url);

        try
        {
            return endpoint.request(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    protected OutboundEndpoint getOutboundEndpoint(String uri, MessageExchangePattern exchangePattern)
        throws MuleException
    {
        EndpointBuilder endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(uri);
        endpointBuilder.setExchangePattern(exchangePattern);
        return muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    }

    /**
     * Packages a mule event for the current request
     */
    protected MuleEvent getEvent(MuleMessage message, OutboundEndpoint endpoint)
        throws MuleException
    {
        MuleSession session = new DefaultMuleSession(message, ((AbstractConnector)endpoint.getConnector()).getSessionHandler(), muleContext);

        if (credentials != null)
        {
            message.setOutboundProperty(MuleProperties.MULE_USER_PROPERTY, "Plain " + credentials.getToken());
        }

        return new DefaultMuleEvent(message, endpoint, session);
    }

    /**
     * Retrieves a ManagedConnection.
     *
     * @return a ManagedConnection instance representing the physical connection to
     *         the EIS
     */

    public MuleManagedConnection getManagedConnection()
    {
        return managedConnection;
    }

    /**
     * Closes the connection.
     */
    public void close() throws ResourceException
    {
        if (managedConnection == null)
        {
            return; // connection is already closed
        }
        managedConnection.removeConnection(this);

        // Send a close event to the App Server
        managedConnection.fireCloseEvent(this);
        managedConnection = null;
    }

    /**
     * Associates connection handle with new managed connection.
     *
     * @param newMc new managed connection
     */

    public void associateConnection(MuleManagedConnection newMc) throws ResourceException
    {
        checkIfValid();
        // dissociate handle from current managed connection
        managedConnection.removeConnection(this);
        // associate handle with new managed connection
        newMc.addConnection(this);
        managedConnection = newMc;
    }

    /**
     * Checks the validity of the physical connection to the EIS.
     *
     * @throws javax.resource.ResourceException in case of any error
     */

    void checkIfValid() throws ResourceException
    {
        if (managedConnection == null)
        {
            throw new ResourceException(
                JcaMessages.objectMarkedInvalid("muleManagedConnection").toString());
        }
    }

    /**
     * Sets the physical connection to the EIS as invalid. The physical connection to
     * the EIS cannot be used any more.
     */

    void invalidate()
    {
        managedConnection = null;
    }
}
