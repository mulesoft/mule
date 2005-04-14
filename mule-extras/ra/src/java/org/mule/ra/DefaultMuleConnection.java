/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.ra;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.security.MuleCredentials;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;

import javax.resource.ResourceException;
import java.util.Map;

/**
 * <code>MuleConnection</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultMuleConnection implements MuleConnection {

    private MuleCredentials credentials;
    private UMOManager manager;
    private MuleManagedConnection managedConnection;

    public DefaultMuleConnection(MuleManagedConnection managedConnection, UMOManager manager, MuleCredentials credentials) {
        this.manager = manager;
        this.credentials = credentials;
        this.managedConnection = managedConnection;
    }

    /**
     * Dispatches an event asynchronously to a endpointUri via a mule server. the Url determines where to dispathc
     * the event to, this can be in the form of
     *
     * @param url               the Mule url used to determine the destination and transport of the message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.  In the case of Jms you could
     *                          set the JMSReplyTo property in these properties.
     * @throws org.mule.umo.UMOException
     */
    public void dispatch(String url, Object payload, Map messageProperties) throws UMOException
    {
        UMOEndpointURI muleEndpoint = new MuleEndpointURI(url);
        UMOMessage message = new MuleMessage(payload, messageProperties);
        UMOEvent event = getEvent(message, muleEndpoint, false);
        try
        {
            event.getSession().dispatchEvent(event);
        } catch (UMOException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new DispatchException(new Message("client", 1), event.getMessage(), event.getEndpoint(), e);
        }
    }

    /**
     * Will receive an event from an endpointUri determined by the url
     *
     * @param url     the Mule url used to determine the destination and transport of the message
     * @param timeout how long to block waiting to receive the event, if set to 0 the receive will
     *                not wait at all and if set to -1 the receive will wait forever
     * @return the message received or null if no message was received
     * @throws org.mule.umo.UMOException
     */
    public UMOMessage receive(String url, long timeout) throws UMOException
    {
        MuleEndpointURI muleEndpoint = new MuleEndpointURI(url);

        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(muleEndpoint, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        try
        {
            UMOMessage message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, timeout);
            return message;
        } catch (Exception e)
        {
            throw new ReceiveException(muleEndpoint, timeout, e);
        }
    }

    /**
     * Packages a mule event for the current request
     *
     * @param message     the event payload
     * @param uri         the destination endpointUri
     * @param synchronous whether the event will be synchronously processed
     * @return the UMOEvent
     * @throws UMOException
     */
    protected UMOEvent getEvent(UMOMessage message, UMOEndpointURI uri, boolean synchronous) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri, UMOEndpoint.ENDPOINT_TYPE_SENDER);

        if (!endpoint.getConnector().isStarted() && manager.isStarted())
        {
            endpoint.getConnector().start();
        }

        try
        {
            UMOSession session = new MuleSession();
            if(credentials!=null) {
                message.setProperty(MuleProperties.MULE_USER_PROPERTY, "Plain " + credentials.getToken());
            }
            MuleEvent event = new MuleEvent(message, endpoint, session, synchronous);

            return event;
        } catch (Exception e)
        {
            throw new DispatchException(new Message(Messages.FAILED_TO_CREATE_X, "Client event"), message, endpoint, e);
        }
    }

    /**
     * Retrieves a ManagedConnection.
     *
     *	@return  a ManagedConnection instance representing the physical
     *           connection to the EIS
     */

    public MuleManagedConnection getManagedConnection()
    {
        return managedConnection;
    }

    /**
     * Closes the connection.
     */
    public void close()	throws ResourceException
    {
        if (managedConnection == null)
	   return;  // connection is already closed
        managedConnection.removeConnection(this);

	// Send a close event to the App Server
        managedConnection.fireCloseEvent(this);
        managedConnection = null;
    }

    /**
     * Associates connection handle with new managed connection.
     *
     * @param newMc  new managed connection
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
     */

    void checkIfValid()	throws ResourceException
    {
        if (managedConnection == null)
	{
            throw new ResourceException(new Message(Messages.X_IS_INVALID, "muleManagedConnection").toString());
        }
    }

    /**
     * Sets the physical connection to the EIS as invalid.
     * The physical connection to the EIS cannot be used any more.
     */

    void invalidate()
    {
        managedConnection = null;
    }
}
