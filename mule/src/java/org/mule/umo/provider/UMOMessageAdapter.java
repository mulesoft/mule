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
 *
 */
package org.mule.umo.provider;

import org.mule.umo.UMOExceptionPayload;

import java.io.Serializable;
import java.util.Iterator;

/**
 * <code>UMOMessageAdapter</code> provides a common abstraction of different message
 * implementations provided by different underlying technologies
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOMessageAdapter extends Serializable
{
    /**
     * Gets a property of the message implementation
     *
     * @param key the key on which to lookup the property value
     * @return the property value or null if the property does not exist
     */
    public Object getProperty(Object key);

    /**
     * Set a property on the message
     *
     * @param key   the key on which to associate the value
     * @param value the property value
     */
    public void setProperty(Object key, Object value);

    /**
     * Removes a property on this message
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not
     * exist
     */
    public Object removeProperty(Object key);


    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception;

    /**
     * @return all properties on this message
     */
    public Iterator getPropertyNames();

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception;

    /**
     * @return the current message
     */
    public Object getPayload();

    /**
     * gets the unique identifier for the message. It's up to the implementation
     * to ensure a unique id
     *
     * @return a unique message id
     * @throws UniqueIdNotSupportedException if the message does not support
     *                                       a unique identifier
     */
    public String getUniqueId() throws UniqueIdNotSupportedException;

    /**
     * Gets a property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public Object getProperty(String name, Object defaultValue);

    /**
     * Gets an integer property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public int getIntProperty(String name, int defaultValue);

    /**
     * Gets a long property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public long getLongProperty(String name, long defaultValue);

    /**
     * Gets a double property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public double getDoubleProperty(String name, double defaultValue);

    /**
     * Gets a boolean property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Sets a boolean property on the event
     *
     * @param name  the property name or key
     * @param value the property value
     */
    public void setBooleanProperty(String name, boolean value);

    /**
     * Sets a integerproperty on the event
     *
     * @param name  the property name or key
     * @param value the property value
     */
    public void setIntProperty(String name, int value);

    /**
     * Sets a long property on the event
     *
     * @param name  the property name or key
     * @param value the property value
     */
    public void setLongProperty(String name, long value);

    /**
     * Sets a double property on the event
     *
     * @param name  the property name or key
     * @param value the property value
     */
    public void setDoubleProperty(String name, double value);

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations
     *
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     * @param id the Id reference for this relationship
     */
    public void setCorrelationId(String id);

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations.
     *
     * The correlationId is associated with the message using the underlying
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getCorrelationId();

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     * @return the sequence number  or -1 if the sequence is not important
     */
    public int getCorrelationSequence();

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     * @param sequence the sequence number  or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence);

    /**
     * Determines how many messages are in the correlation group
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize();

    /**
     * Determines how many messages are in the correlation group
     * @param size the total messages in this group or -1 if the size is not known
     */
    public void setCorrelationGroupSize(int size);

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     * @param replyTo the endpointUri url to reply to
     */
    public void setReplyTo(Object replyTo);

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     * @return the endpointUri url to reply to or null if one has not been set
     */
    public Object getReplyTo();

    /**
     * If an error occurred during the processing of this message this
     * will return a ErrorPayload that contains the root exception and
     * Mule error code, plus any otherr releated info
     * @return
     */
    public UMOExceptionPayload getExceptionPayload();

    /**
     * If an error occurs while processing this message, a ErrorPayload is
     * attached which contains the root exception and
     * Mule error code, plus any otherr releated info
     * @param payload The exception payloaad to attach to this message
     */
    public void setExceptionPayload(UMOExceptionPayload payload);
}