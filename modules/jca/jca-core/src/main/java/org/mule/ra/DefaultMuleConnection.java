/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.extras.client.i18n.ClientMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.AbstractConnector;
import org.mule.ra.i18n.JcaMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;

import java.util.Map;

import javax.resource.ResourceException;

/**
 * <code>MuleConnection</code> TODO
 */
public class DefaultMuleConnection implements MuleConnection
{
    private final MuleCredentials credentials;
    private final UMOManagementContext manager;
    private MuleManagedConnection managedConnection;

    public DefaultMuleConnection(MuleManagedConnection managedConnection,
                                 UMOManagementContext manager,
                                 MuleCredentials credentials)
    {
        this.manager = manager;
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
     * @throws org.mule.umo.UMOException
     */
    public void dispatch(String url, Object payload, Map messageProperties) throws UMOException
    {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, url, false);
        try
        {
            event.getSession().dispatchEvent(event);
        }
        catch (UMOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DispatchException(
                ClientMessages.failedToDispatchClientEvent(),
                event.getMessage(), event.getEndpoint(), e);
        }
    }

    /**
     * Sends an object (payload) synchronous to the given url and returns a
     * UMOMessage response back.
     * 
     * @param url the Mule url used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @return a umomessage response.
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage send(String url, Object payload, Map messageProperties) throws UMOException
    {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, url, true);

        UMOMessage response;
        try
        {
            response = event.getSession().sendEvent(event);
        }
        catch (UMOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DispatchException(
                ClientMessages.failedToDispatchClientEvent(), 
                event.getMessage(), event.getEndpoint(), e);
        }
        return response;
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
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage receive(String url, long timeout) throws UMOException
    {
        UMOImmutableEndpoint endpoint = manager.getRegistry().lookupEndpointFactory().getOutboundEndpoint(url);

        try
        {
            return endpoint.receive(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    /**
     * Packages a mule event for the current request
     * 
     * @param message the event payload
     * @param uri the destination endpointUri
     * @param synchronous whether the event will be synchronously processed
     * @return the UMOEvent
     * @throws UMOException in case of Mule error
     */
    protected UMOEvent getEvent(UMOMessage message, String uri, boolean synchronous)
        throws UMOException
    {
        UMOImmutableEndpoint endpoint = manager.getRegistry().lookupEndpointFactory().getOutboundEndpoint(uri);
        //UMOConnector connector = endpoint.getConnector();

//        if (!connector.isStarted() && manager.isStarted())
//        {
//            connector.start();
//        }

        try
        {
            UMOSession session = new MuleSession(message,
                ((AbstractConnector)endpoint.getConnector()).getSessionHandler());

            if (credentials != null)
            {
                message.setProperty(MuleProperties.MULE_USER_PROPERTY, "Plain " + credentials.getToken());
            }

            return new MuleEvent(message, endpoint, session, synchronous);
        }
        catch (Exception e)
        {
            throw new DispatchException(
                CoreMessages.failedToCreate("Client event"), message, endpoint, e);
        }
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
