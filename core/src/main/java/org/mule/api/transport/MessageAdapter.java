/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transport;

import org.mule.api.ExceptionPayload;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

/**
 * <code>MessageAdapter</code> provides a common abstraction of different
 * message implementations provided by different underlying technologies.
 */
public interface MessageAdapter extends Serializable
{

    /**
     * Adds a map of properties to be associated with this message
     * 
     * @param properties the properties add to this message
     */
    void addProperties(Map properties);

    /**
     * Adds a map of properties to be associated with this message
     *
     * @param properties the properties add to this message
     * @param scope the scope in which the proeprties should be added
     */
    void addProperties(Map properties, PropertyScope scope);

    /**
     * Removes all properties on this message
     */
    void clearProperties();

    /**
     * Gets a property of the message implementation
     * 
     * @param key the key on which to lookup the property value
     * @return the property value or null if the property does not exist
     */
    Object getProperty(String key);

    /**
     * Set a property on the message
     * 
     * @param key the key on which to associate the value
     * @param value the property value
     */
    void setProperty(String key, Object value);

    /**
     * Set a property on the message
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param scope The scope at which to set the property at
     * @see PropertyScope
     */
    void setProperty(String key, Object value, PropertyScope scope);
    /**
     * Removes a property on this message
     * 
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     */
    Object removeProperty(String key);

    /**
     * @return all property keys on this message
     */
    Set getPropertyNames();

    /**
     * Gets all property names in a given scope
     * @param scope the scope of property names
     * @return all property keys on this message
     */
    Set getPropertyNames(PropertyScope scope);

    /**
     * @return the current message
     */
    Object getPayload();

    /**
     * gets the unique identifier for the message. It's up to the implementation to
     * ensure a unique id
     * 
     * @return a unique message id. The Id should never be null. If the underlying
     *         transport does not have the notion of a message Id, one shuold be
     *         generated. The generated Id should be a UUID.
     */
    String getUniqueId();

    /**
     * Gets a property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    Object getProperty(String name, Object defaultValue);

    /**
     * Gets a property from the message with a given scope
     *
     * @param name the name or key of the property
     * @param scope The scope of the property to retrieve
     * @return the property value or the defaultValue if the property does not exist in the specificed scope
     */
    Object getProperty(String name, PropertyScope scope);
    
    /**
     * Gets an integer property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    int getIntProperty(String name, int defaultValue);

    /**
     * Gets a long property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    long getLongProperty(String name, long defaultValue);

    /**
     * Gets a double property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    double getDoubleProperty(String name, double defaultValue);

    /**
     * Gets a boolean property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Sets a boolean property on the message
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setBooleanProperty(String name, boolean value);

    /**
     * Sets a integerproperty on the message
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setIntProperty(String name, int value);

    /**
     * Sets a long property on the message
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setLongProperty(String name, long value);

    /**
     * Sets a double property on the message
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setDoubleProperty(String name, double value);

    /**
     * Gets a String property from the message
     * 
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     */
    String getStringProperty(String name, String defaultValue);

    /**
     * Sets a String property on the message
     * 
     * @param name the property name or key
     * @param value the property value
     */
    void setStringProperty(String name, String value);

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations <p/> transport protocol.
     * As such not all messages will support the notion of a correlationId i.e. tcp
     * or file. In this situation the correlation Id is set as a property of the
     * message where it's up to developer to keep the association with the message.
     * For example if the message is serialised to xml the correlationId will be
     * available in the message.
     * 
     * @param id the Id reference for this relationship
     */
    void setCorrelationId(String id);

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations. <p/> The correlationId
     * is associated with the message using the underlying transport protocol. As
     * such not all messages will support the notion of a correlationId i.e. tcp or
     * file. In this situation the correlation Id is set as a property of the message
     * where it's up to developer to keep the association with the message. For
     * example if the message is serialised to xml the correlationId will be
     * available in the message.
     * 
     * @return the correlationId for this message or null if one hasn't been set
     */
    String getCorrelationId();

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @return the sequence number or -1 if the sequence is not important
     */
    int getCorrelationSequence();

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @param sequence the sequence number or -1 if the sequence is not important
     */
    void setCorrelationSequence(int sequence);

    /**
     * Determines how many messages are in the correlation group
     * 
     * @return total messages in this group or -1 if the size is not known
     */
    int getCorrelationGroupSize();

    /**
     * Determines how many messages are in the correlation group
     * 
     * @param size the total messages in this group or -1 if the size is not known
     */
    void setCorrelationGroupSize(int size);

    /**
     * Sets a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     * 
     * @param replyTo the endpointUri url to reply to
     */
    void setReplyTo(Object replyTo);

    /**
     * Returns a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     * 
     * @return the endpointUri url to reply to or null if one has not been set
     */
    Object getReplyTo();

    /**
     * If an error occurred during the processing of this message this will return a
     * ErrorPayload that contains the root exception and Mule error code, plus any
     * other releated info
     *
     * @return The exception payload (if any) attached to this message
     */
    ExceptionPayload getExceptionPayload();

    /**
     * If an error occurs while processing this message, a ErrorPayload is attached
     * which contains the root exception and Mule error code, plus any other releated
     * info.
     *
     * @param payload The exception payload to attach to this message
     */
    void setExceptionPayload(ExceptionPayload payload);

    /**
     * Allows for arbitary data attachments to be associated with the Message. These attachements work in the
     * same way that email attachments work. Attachments can be binary or text
     * @param name the name to associate with the attachment
     * @param dataHandler The attachment datahandler to use. This will be used to interract with the attachment data
     * @throws Exception
     * @see javax.activation.DataHandler
     */
    void addAttachment(String name, DataHandler dataHandler) throws Exception;

    /**
     * Remove an attahcment form this message with the specifed name
     * @param name the name of the attachment to remove. If the attachment does not exist, the request may be ignorred
     * @throws Exception different messaging systems handle attachments differnetly, as such some will throw an exception
     * if an attahcment does dot exist.
     */
    void removeAttachment(String name) throws Exception;

    /**
     * Retrieve an attachment with the given name. If the attachment does not exist, null will be returned
     * @param name the name of the attachment to retrieve
     * @return the attachment with the given name or null if the attachment does not exist
     * @see javax.activation.DataHandler
     */
    DataHandler getAttachment(String name);

    /**
     * Returns a set of the names of the attachments on this message. If there are no attachments an empty set will be
     * returned.
     * @return a set of the names of the attachments on this message. If there are no attachments an empty set will be
     * returned.
     */
    Set getAttachmentNames();

    /**
     * Gets the encoding for the current message. For potocols that send encoding
     * information with the message, this method should be overriden to expose the
     * transport encoding, otherwise the default encoding in the Mule configuration
     * will be used.
     * 
     * @return the encoding for this message. This method must never return null
     */
    String getEncoding();

    /**
     * Sets the encoding for this message
     * 
     * @param encoding the encoding to use
     */
    void setEncoding(String encoding);

    /**
     * Perform any clean up operations on the message resource.
     * Typically this is used to esure that a message stream is closed
     */
    void release();
}
