/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.security.SecurityContext;
import org.mule.api.service.Service;

import java.io.Serializable;
import java.util.Iterator;

/**
 * <code>MuleSession</code> is the context in which a request is executed. The
 * session manages the marshalling of events to and from components This object is
 * not usually referenced by client code directly. If needed Components should manage
 * events via the <code>MuleEventContext</code> which is obtainable via the
 * <code>UMOManager</code> or by implementing
 * <code>org.mule.api.lifecycle.Callable</code>.
 */

public interface MuleSession extends Serializable
{
    /**
     * Returns the Service associated with the session in its current execution
     * 
     * @return the Service associated with the session in its current execution
     * @see org.mule.api.service.Service
     */
    Service getService();

    /**
     * This will send an event via the configured outbound endpoint on the service
     * for this session
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws org.mule.api.MuleException if there is no outbound endpoint configured
     *             on the service or the events fails during dispatch
     */
    MuleMessage sendEvent(MuleMessage message) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param event the event to process
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleEvent event) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @return the return Message from the call or null if there was no result
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    MuleMessage sendEvent(MuleMessage message, String endpointName) throws MuleException;

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the service for this session
     * 
     * @param message the message to send
     * @throws MuleException if there is no outbound endpoint configured on the
     *             service or the events fails during dispatch
     */
    void dispatchEvent(MuleMessage message) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param event the event message payload to send first on the service
     *            configuration and then on the mule manager configuration
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleEvent event) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleMessage message, OutboundEndpoint endpoint) throws MuleException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule component in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the service configuration and then on the
     *            mule manager configuration
     * @throws MuleException if the event fails to be processed by the service or
     *             the transport for the endpoint
     */
    void dispatchEvent(MuleMessage message, String endpointName) throws MuleException;

    /**
     * Requests a synchronous receive of an event on the service
     * 
     * @param endpoint the endpoint identifing the endpointUri on ewhich the event
     *            will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws MuleException if the request operation fails
     */
    MuleMessage requestEvent(InboundEndpoint endpoint, long timeout) throws MuleException;

    /**
     * Requests a synchronous receive of an event on the service
     * 
     * @param endpointName the endpoint name identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws MuleException if the request operation fails
     */
    MuleMessage requestEvent(String endpointName, long timeout) throws MuleException;

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @return true if the service is functioning properly, false otherwise
     */
    boolean isValid();

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @param value true if the service is functioning properly, false otherwise
     */
    void setValid(boolean value);

    /**
     * Creates an outbound event for this session
     * 
     * @param message the event messgae payload
     * @param endpoint the endpoint to send/dispatch through
     * @param previousEvent the previous event (if any) on this session
     * @return the event to send/dispatch
     * @throws MuleException if the evnet cannot be created
     */
    MuleEvent createOutboundEvent(MuleMessage message, OutboundEndpoint endpoint, MuleEvent previousEvent)
        throws MuleException;

    /**
     * Returns the unique id for this session
     * 
     * @return the unique id for this session
     */
    String getId();

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @param context the context for this session or null if the request is not
     *            secure.
     */
    void setSecurityContext(SecurityContext context);

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    SecurityContext getSecurityContext();

    /**
     * Will set a session level property. These will either be stored and retrieved
     * using the underlying transport mechanism of stored using a default mechanism
     * 
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    void setProperty(Object key, Object value);

    /**
     * Will retrieve a session level property.
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    Object getProperty(Object key);

    /**
     * Will retrieve a session level property and remove it from the session
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    Object removeProperty(Object key);

    /**
     * Returns an iterater of property keys for the session properties on this
     * session
     * 
     * @return an iterater of property keys for the session properties on this
     *         session
     */
    Iterator getPropertyNames();

}
