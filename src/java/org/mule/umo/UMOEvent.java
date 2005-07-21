/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/umo/UMOEvent.java,v 1.9
 * 2003/12/02 22:43:27 rossmason Exp $ $Revision$ $Date: 2003/12/02
 * 22:43:27 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.umo;

import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.transformer.TransformerException;

import java.io.OutputStream;
import java.util.Map;

/**
 * <code>UMOEvent</code> represents any data event occuring in the Mule
 * environment. All data sent or received within the mule environment will be
 * passed between components as an UMOEvent. <p/> <p/> The UMOEvent holds a
 * UMOMessage payload and provides helper methods for obtaining the data in a
 * format that the receiving Mule UMO understands. The event can also maintain
 * any number of properties that can be set and retrieved by Mule UMO
 * components.
 * 
 * @see UMOMessage
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEvent
{
    int TIMEOUT_WAIT_FOREVER = 0;
    int TIMEOUT_DO_NOT_WAIT = -1;

    /**
     * Returns the message payload for this event
     * 
     * @return the message payload for this event
     */
    UMOMessage getMessage();

    public UMOCredentials getCredentials();

    /**
     * Reterns the conents of the message as a byte array.
     * 
     * @return the conents of the message as a byte array
     * @throws UMOException if the message cannot be converted into an array of
     *             bytes
     */
    byte[] getMessageAsBytes() throws UMOException;

    /**
     * Returns the message transformed into it's recognised or expected format.
     * The transformer used is the one configured on the endpoint through which
     * this event was received.
     * 
     * @return the message transformed into it's recognised or expected format.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    Object getTransformedMessage() throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into an array of bytes. The transformer used is the one
     * configured on the endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format
     *         as an array of bytes.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    byte[] getTransformedMessageAsBytes() throws TransformerException;

    /**
     * Returns the message transformed into it's recognised or expected format
     * and then into a String. The transformer used is the one configured on the
     * endpoint through which this event was received.
     * 
     * @return the message transformed into it's recognised or expected format
     *         as a Strings.
     * @throws TransformerException if a failure occurs in the transformer
     * @see org.mule.umo.transformer.UMOTransformer
     */
    String getTransformedMessageAsString() throws TransformerException;

    /**
     * Returns the message contents as a string
     * 
     * @return the message contents as a string
     * @throws UMOException if the message cannot be converted into a string
     */
    String getMessageAsString() throws UMOException;

    /**
     * Every event in the system is assigned a universally unique id (UUID).
     * 
     * @return the unique identifier for the event
     */
    String getId();

    /**
     * Gets a property associated with the current event. Calling this method is
     * equivilent to calling <code>event.getMessage().getProperty(...)</code>
     * 
     * @param name the property name
     * @return the property value or null if the property does not exist
     */
    Object getProperty(String name);

    /**
     * Gets a property associated with the current event. Calling this method is
     * equivilent to calling
     * <code>event.getMessage().getProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    Object getProperty(String name, Object defaultValue);

    /**
     * Gets an Integer property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getIntProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    int getIntProperty(String name, int defaultValue);

    /**
     * Gets a Long property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getLongProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    long getLongProperty(String name, long defaultValue);

    /**
     * Gets a Double property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getDoubleProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    double getDoubleProperty(String name, double defaultValue);

    /**
     * Gets a Boolean property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().getbooleanProperty(..., ...)</code>
     * 
     * @param name the property name
     * @param defaultValue a default value if the property doesn't exist in the
     *            event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Sets a property associated with the current event. Calling this method is
     * equivilent to calling
     * <code>event.getMessage().setProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setProperty(String name, Object value);

    /**
     * Sets a Boolean property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setBooleanProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setBooleanProperty(String name, boolean value);

    /**
     * Sets an Integer property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setIntProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setIntProperty(String name, int value);

    /**
     * Sets a Long property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setLongProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setLongProperty(String name, long value);

    /**
     * Sets a Double property associated with the current event. Calling this
     * method is equivilent to calling
     * <code>event.getMessage().setDoubleProperty(..., ...)</code>
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setDoubleProperty(String name, double value);

    /**
     * Returns a map of properties associated with the event
     * 
     * @return a map of properties on the event
     */
    Map getProperties();

    /**
     * Gets the endpoint associated with this event
     * 
     * @return the endpoint associated with this event
     */
    UMOEndpoint getEndpoint();

    /**
     * Retrieves the component session for the current event
     * 
     * @return the component session for the event
     */
    UMOSession getSession();

    /**
     * Retrieves the component for the current event
     * 
     * @return the component for the event
     */
    UMOComponent getComponent();

    /**
     * Determines whether the default processing for this event will be
     * executed. By default, the Mule server will route events according to a
     * components configuration. The user can override this behaviour by
     * obtaining a reference to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the UMOEventContext
     * for the current thread. The user can programmatically control how events
     * are dispached.
     * 
     * @return Returns true is the user has set stopFurtherProcessing.
     * @see org.mule.umo.manager.UMOManager
     * @see UMOEventContext
     * @see org.mule.umo.lifecycle.Callable
     */
    boolean isStopFurtherProcessing();

    /**
     * Determines whether the default processing for this event will be
     * executed. By default, the Mule server will route events according to a
     * components configuration. The user can override this behaviour by
     * obtaining a reference to the Event context, either by implementing
     * <code>org.mule.umo.lifecycle.Callable</code> or calling
     * <code>UMOManager.getEventContext</code> to obtain the UMOEventContext
     * for the current thread. The user can programmatically control how events
     * are dispached.
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
     * An outputstream the can optionally be used write response data to an
     * incoming message.
     * 
     * @return an output strem if one has been made available by the message
     *         receiver that received the message
     */
    OutputStream getOutputStream();

    /**
     * Removes a property from the event
     * 
     * @param key the property key to remove
     * @return the removed property or null if the property was not found or if
     *         the underlying message does not return the removed property
     */
    Object removeProperty(Object key);
}
