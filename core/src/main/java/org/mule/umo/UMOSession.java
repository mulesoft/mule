/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.security.UMOSecurityContext;

import java.io.Serializable;
import java.util.Iterator;

/**
 * <code>UMOSession</code> is the context in which a request is executed. The
 * session manages the marshalling of events to and from components This object is
 * not usually referenced by client code directly. If needed Components should manage
 * events via the <code>UMOEventContext</code> which is obtainable via the
 * <code>UMOManager</code> or by implementing
 * <code>org.mule.umo.lifecycle.Callable</code>.
 */

public interface UMOSession extends Serializable
{
    /**
     * Returns the UMOComponent associated with the session in its current execution
     * 
     * @return the UMOComponent associated with the session in its current execution
     * @see org.mule.umo.UMOComponent
     */
    UMOComponent getComponent();

    /**
     * This will send an event via the configured outbound endpoint on the component
     * for this session
     * 
     * @param message the message to send
     * @return the result of the send if any
     * @throws org.mule.umo.UMOException if there is no outbound endpoint configured
     *             on the component or the events fails during dispatch
     */
    UMOMessage sendEvent(UMOMessage message) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param event the event to process
     * @return the return Message from the call or null if there was no result
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    UMOMessage sendEvent(UMOEvent event) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint to disptch the event through
     * @return the return Message from the call or null if there was no result
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    UMOMessage sendEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * synchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the component configuration and then on the
     *            mule manager configuration
     * @return the return Message from the call or null if there was no result
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    UMOMessage sendEvent(UMOMessage message, String endpointName) throws UMOException;

    /**
     * This will dispatch an event asynchronously via the configured outbound
     * endpoint on the component for this session
     * 
     * @param message the message to send
     * @throws UMOException if there is no outbound endpoint configured on the
     *             component or the events fails during dispatch
     */
    void dispatchEvent(UMOMessage message) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param event the event message payload to send first on the component
     *            configuration and then on the mule manager configuration
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    void dispatchEvent(UMOEvent event) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpoint The endpoint name to disptch the event through
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    void dispatchEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException;

    /**
     * Depending on the session state this methods either Passes an event
     * asynchronously to the next available Mule UMO in the pool or via the endpoint
     * configured for the event
     * 
     * @param message the event message payload to send
     * @param endpointName The endpoint name to disptch the event through. This will
     *            be looked up first on the component configuration and then on the
     *            mule manager configuration
     * @throws UMOException if the event fails to be processed by the component or
     *             the transport for the endpoint
     */
    void dispatchEvent(UMOMessage message, String endpointName) throws UMOException;

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpoint the endpoint identifing the endpointUri on ewhich the event
     *            will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws UMOException if the request operation fails
     */
    UMOMessage receiveEvent(UMOImmutableEndpoint endpoint, long timeout) throws UMOException;

    /**
     * Requests a synchronous receive of an event on the component
     * 
     * @param endpointName the endpoint name identifing the endpointUri on ewhich the
     *            event will be received
     * @param timeout time in milliseconds before the request timesout
     * @return The requested event or null if the request times out
     * @throws UMOException if the request operation fails
     */
    UMOMessage receiveEvent(String endpointName, long timeout) throws UMOException;

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @return true if the component is functioning properly, false otherwise
     */
    boolean isValid();

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @param value true if the component is functioning properly, false otherwise
     */
    void setValid(boolean value);

    /**
     * Creates an outbound event for this session
     * 
     * @param message the event messgae payload
     * @param endpoint the endpoint to send/dispatch through
     * @param previousEvent the previous event (if any) on this session
     * @return the event to send/dispatch
     * @throws UMOException if the evnet cannot be created
     */
    UMOEvent createOutboundEvent(UMOMessage message, UMOImmutableEndpoint endpoint, UMOEvent previousEvent)
        throws UMOException;

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
    void setSecurityContext(UMOSecurityContext context);

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    UMOSecurityContext getSecurityContext();

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
