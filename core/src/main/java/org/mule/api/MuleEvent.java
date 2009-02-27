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

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.Credentials;
import org.mule.api.service.Service;
import org.mule.api.transformer.TransformerException;

import java.io.OutputStream;

/**
 * <code>MuleEvent</code> represents any data event occuring in the Mule
 * environment. All data sent or received within the mule environment will be passed
 * between components as an MuleEvent. <p/> <p/> The MuleEvent holds a MuleMessage
 * payload and provides helper methods for obtaining the data in a format that the
 * receiving Mule component understands. The event can also maintain any number of
 * properties that can be set and retrieved by Mule components.
 * 
 * @see MuleMessage
 */
public interface MuleEvent
{
    int TIMEOUT_WAIT_FOREVER = 0;
    int TIMEOUT_DO_NOT_WAIT = -1;
    int TIMEOUT_NOT_SET_VALUE = Integer.MIN_VALUE;

    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    MuleMessage getMessage();

    Credentials getCredentials();

    /**
     * Reterns the conents of the message as a byte array.
     * 
     * @return the conents of the message as a byte array
     * @throws MuleException if the message cannot be converted into an array of bytes
     */
    byte[] getMessageAsBytes() throws MuleException;

    /**
     * Transforms the message into it's recognised or expected format. The
     * transformer used is the one configured on the endpoint through which this
     * event was received.
     * 
     * @return the message transformed into it's recognised or expected format.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    Object transformMessage() throws TransformerException;

    /**
     * Transforms the message into the requested format. The transformer used is 
     * the one configured on the endpoint through which this event was received.
     * 
     * @param outputType The requested output type.
     * @return the message transformed into it's recognised or expected format.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    Object transformMessage(Class outputType) throws TransformerException;

    /**
     * Transforms the message into it's recognised or expected format and then 
     * into an array of bytes. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format as an
     *         array of bytes.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    byte[] transformMessageToBytes() throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format and
     * then into a String. The transformer used is the one configured on the endpoint
     * through which this event was received. If necessary this will use the encoding
     * set on the event
     * 
     * @return the message transformed into it's recognised or expected format as a
     *         Strings.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.api.transformer.Transformer
     */
    String transformMessageToString() throws TransformerException;

    /**
     * Returns the message contents as a string If necessary this will use the
     * encoding set on the event
     * 
     * @return the message contents as a string
     * @throws MuleException if the message cannot be converted into a string
     */
    String getMessageAsString() throws MuleException;

    /**
     * Returns the message contents as a string
     * 
     * @param encoding the encoding to use when converting the message to string
     * @return the message contents as a string
     * @throws MuleException if the message cannot be converted into a string
     */
    String getMessageAsString(String encoding) throws MuleException;

    /**
     * Every event in the system is assigned a universally unique id (UUID).
     * 
     * @return the unique identifier for the event
     */
    String getId();

    /**
     * Gets a property associated with the current event. This method will check all property scopes on the currnet message
     * and the current session
     * 
     * @param name the property name
     * @return the property value or null if the property does not exist
     */
    Object getProperty(String name);

    /**
     * Gets a property associated with the current event. This method will check all property scopes on the currnet message
     * and the current session
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    Object getProperty(String name, Object defaultValue);

    /**
     * Gets the endpoint associated with this event
     * 
     * @return the endpoint associated with this event
     */
    ImmutableEndpoint getEndpoint();

    /**
     * Retrieves the service session for the current event
     * 
     * @return the service session for the event
     */
    MuleSession getSession();

    /**
     * Retrieves the service for the current event
     * 
     * @return the service for the event
     */
    Service getService();

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the MuleEvent context, either by implementing
     * <code>org.mule.api.lifecycle.Callable</code> or calling
     * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for
     * the current thread. The user can programmatically control how events are
     * dispached.
     * 
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.api.MuleContext
     * @see MuleEventContext
     * @see org.mule.api.lifecycle.Callable
     */
    boolean isStopFurtherProcessing();

    /**
     * Determines whether the default processing for this event will be executed. By
     * default, the Mule server will route events according to a components
     * configuration. The user can override this behaviour by obtaining a reference
     * to the MuleEvent context, either by implementing
     * <code>org.mule.api.lifecycle.Callable</code> or calling
     * <code>RequestContext.getEventContext</code> to obtain the MuleEventContext for
     * the current thread. The user can programmatically control how events are
     * dispached.
     * 
     * @param stopFurtherProcessing the value to set.
     */
    void setStopFurtherProcessing(boolean stopFurtherProcessing);

    /**
     * Determines whether the was sent synchrounously or not
     * 
     * @return true if the event is synchronous
     */
    boolean isSynchronous();

    /**
     * Determines whether the was sent synchrounously or not
     * 
     * @param value true if the event is synchronous
     */
    void setSynchronous(boolean value);

    /**
     * The number of milliseconds to wait for a return event when running
     * synchronously. 0 wait forever -1 try and receive, but do not wait or a
     * positive millisecond value
     * 
     * @return the event timeout in milliseconds
     */
    int getTimeout();

    /**
     * The number of milliseconds to wait for a return event when running
     * synchronously. 0 wait forever -1 try and receive, but do not wait or a
     * positive millisecod value
     * 
     * @param timeout the event timeout in milliseconds
     */
    void setTimeout(int timeout);

    /**
     * An outputstream the can optionally be used write response data to an incoming
     * message.
     * 
     * @return an output strem if one has been made available by the message receiver
     *         that received the message
     */
    OutputStream getOutputStream();

    /**
     * Gets the encoding for this message.
     * 
     * @return the encoding for the event. This must never return null.
     */
    String getEncoding();

    /**
     * Returns the muleContext for the Mule node that this event was received in
     * @return the muleContext for the Mule node that this event was received in
     */
    MuleContext getMuleContext();
}
