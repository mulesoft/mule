/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

/**
 * <code>MuleMessage</code> represents a message payload. The Message comprises of
 * the payload itself and properties associated with the payload.
 */

public interface MuleMessage extends Serializable
{
    /**
     * Adds a map of properties to be associated with this message
     *
     * @param properties the properties add to this message
     * @deprecated use {@link #addProperties(java.util.Map, org.mule.api.transport.PropertyScope)} instead
     */
    @Deprecated
    void addProperties(Map<String, Object> properties);

    /**
     * Adds a map of properties to be associated with this message
     *
     * @param properties the properties add to this message
     * @param scope the scope in which the properties should be added
     */
    void addProperties(Map<String, Object> properties, PropertyScope scope);

    /**
     * Removes all properties on this message in the {@link org.mule.api.transport.PropertyScope#INVOCATION} and
     * {@link org.mule.api.transport.PropertyScope#OUTBOUND}.
     * @deprecated use {@link #clearProperties(org.mule.api.transport.PropertyScope)} instead
     */
    @Deprecated
    void clearProperties();

    /**
     * Removes all properties on this message in the given scope. Note that the INBOUND scope is
     * read-only, so attempting to clear this scopee will result in an UnsupportedOperationException.
     *
     * @param scope the property scope to clear
     * @throws UnsupportedOperationException if scope specified is {@link org.mule.api.transport.PropertyScope#INBOUND}
     */
    void clearProperties(PropertyScope scope);
    /**
     /**
     *
     * @deprecated use the overloaded version with an explicit lookup scope. This method will
     * now use only the outbound scope.
     * @see #getInboundProperty(String)
     * @see #getOutboundProperty(String)
     * @see #getInvocationProperty(String)
     * @see #getSessionProperty(String)
     */
    @Deprecated
    Object getProperty(String key);

    /**
     * Set a property on the message. This method will now set a value on the outbound scope only.
     * @deprecated use {@link #setProperty(String, Object, org.mule.api.transport.PropertyScope)} or
     * preferrably any of the scope-specific set methods.
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @see #setInvocationProperty(String, Object)
     * @see #setOutboundProperty(String, Object)
     * @see #setSessionProperty(String, Object)
     */
    @Deprecated
    void setProperty(String key, Object value);

    /**
     * @see #setProperty(String, Object, org.mule.api.transport.PropertyScope)
     */
    void setInvocationProperty(String key, Object value);

    /**
     * @see #setProperty(String, Object, org.mule.api.transport.PropertyScope)
     */
    void setOutboundProperty(String key, Object value);

    /**
     * Set a property on the message. End-users should prefer more
     * scope-specific methods for readability. This one is more intended for programmatic
     * scope manipulation and Mule internal use.
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param scope The scope at which to set the property at
     * @see PropertyScope
     * @see #setInvocationProperty(String, Object)
     * @see #setOutboundProperty(String, Object)
     * @see #setSessionProperty(String, Object)
     */
    void setProperty(String key, Object value, PropertyScope scope);

    /**
     * Removes a property on this message.
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     * @deprecated use {@link #removeProperty(String, org.mule.api.transport.PropertyScope)}
     */
    @Deprecated
    Object removeProperty(String key);

    /**
     * Removes a property on this message from the specified scope only.
     *
     * @param key the property key to remove
     * @param scope The scope at which to set the property at
     * @return the removed property value or null if the property did not exist
     */
    Object removeProperty(String key, PropertyScope scope);

    /**
     * @return all property keys on this message.
     * @since 3.0 only the outbound scope properties are returned
     * @deprecated use {@link #getPropertyNames(org.mule.api.transport.PropertyScope)}
     */
    @Deprecated
    Set<String> getPropertyNames();

    /**
     * Gets all property names in a given scope. Prefer using one of the convenience
     * scope-aware methods instead, this one is meant for internal access mostly.
     * @param scope the scope of property names
     * @return all property keys on this message in the given scope
     * @see #getInvocationPropertyNames()
     * @see #getInboundPropertyNames()
     * @see #getOutboundPropertyNames()
     * @see #getSessionPropertyNames()
     */
    Set<String> getPropertyNames(PropertyScope scope);

    Set<String> getInvocationPropertyNames();
    Set<String> getInboundPropertyNames();
    Set<String> getOutboundPropertyNames();

    @Deprecated
    Set<String> getSessionPropertyNames();


    /**
     * @return the current message
     */
    Object getPayload();

    /**
     * gets the unique identifier for the message. It's up to the implementation to
     * ensure a unique id
     *
     * @return a unique message id. The Id should never be null. If the underlying
     *         transport does not have the notion of a message Id, one should be
     *         generated. The generated Id should be a UUID.
     */
    String getUniqueId();

    /**
     * gets an identifier that is the same among parent and child messages
     *
     * @return a message id for the group of descendant messages. The Id should never be null.
     */
    String getMessageRootId();

    /**
     * set the root ID for the message
     */
    void setMessageRootId(String rootId);

    /**
     * copy the message root id from parent to child
     */
    void propagateRootId(MuleMessage parent);

    /**
     * Gets a property from the message
     *
     * @param name the name or key of the property. This must be non-null.
     * @param defaultValue a default value if the property doesn't exist in the event. This can be null.
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use scope-aware methods instead
     * @see #getInboundProperty(String)
     * @see #getOutboundProperty(String)
     * @see #getInvocationProperty(String)
     * @see #getSessionProperty(String)
     */
    @Deprecated
    Object getProperty(String name, Object defaultValue);

    /**
     * Gets a property from the message with a given scope. End-users should prefer more
     * scope-specific methods for readability. This one is more intended for programmatic
     * scope manipulation and Mule internal use.
     *
     * @param name the name or key of the property. This must be non-null.
     * @param scope The scope of the property to retrieve. This must be non-null.
     * @return the property value or null if the property does not exist in the specified scope
     * @see #getInboundProperty(String)
     * @see #getOutboundProperty(String)
     * @see #getInvocationProperty(String)
     * @see #getSessionProperty(String)
     */
    <T> T getProperty(String name, PropertyScope scope);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getInboundProperty(String name, T defaultValue);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getInboundProperty(String name);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getInvocationProperty(String name, T defaultValue);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getInvocationProperty(String name);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getOutboundProperty(String name, T defaultValue);

    /**
     * @see #getProperty(String, org.mule.api.transport.PropertyScope, Object)
     */
    <T> T getOutboundProperty(String name);

    /**
     * This method was added with the introduction of Property scopes.  However, this method should
     * rarely be used.  Instead, the scoped accessors should be used.  Mule does not use this method internally
     * and may be deprecated in future versions
     *
     * The Scopes will be checked in the following order, with the first match being returned -
     * <ul>
     * <li>Outbound</li>
     * <li>Invocation</li>
     * <li>Session</li>
     * <li>Inbound</li>
     * </ul>
     *
     * @param name the name of the property to look for
     * @param defaultValue the default value that will be returned if the property is not found
     * @param <T> The Type of the property value that will be returned
     * @return TThe property value from the first scope that had the property set, or the 'defaultValue' if the property was
     * not found in any scope
     * @since 3.0
     */
    <T> T findPropertyInAnyScope(String name, T defaultValue);

    /**
     * Gets a property from the message with a given scope and provides a default value if the property is not
     * present on the message in the scope specified.  The method will also type check against the default value
     * to ensure that the value is of the correct type.  If null is used for the default value no type checking is
     * done.
     * @param name the name or key of the property. This must be non-null.
     * @param scope The scope of the property to retrieve.  This must be non-null.
     * @param defaultValue the value to return if the property is not in the scope provided. Can be null
     * @param <T> the defaultValue type ,this is used to validate the property value type
     * @return the property value or the defaultValue if the property does not exist in the specified scope
     * @throws IllegalArgumentException if the value for the property key is not assignable from the defaultValue type
     */
    <T> T getProperty(String name, PropertyScope scope, T defaultValue);

    /**
     * Gets an integer property from the message
     *
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use {@link #getInboundProperty(String, Object)} instead
     */
    @Deprecated
    int getIntProperty(String name, int defaultValue);

    /**
     * Gets a long property from the message
     *
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use {@link #getInboundProperty(String, Object)} instead
     */
    @Deprecated
    long getLongProperty(String name, long defaultValue);

    /**
     * Gets a double property from the message
     *
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use {@link #getInboundProperty(String, Object)} instead
     */
    @Deprecated
    double getDoubleProperty(String name, double defaultValue);

    /**
     * Gets a String property from the message
     *
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use {@link #getInboundProperty(String, Object)} instead
     */
    @Deprecated
    String getStringProperty(String name, String defaultValue);

    /**
     * Gets a boolean property from the message
     *
     * @param name the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not exist
     * @deprecated use {@link #getInboundProperty(String, Object)} instead
     */
    @Deprecated
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Sets a boolean property on the message
     *
     * @param name the property name or key
     * @param value the property value
     * @deprecated use {@link #setOutboundProperty(String, Object)} instead
     */
    @Deprecated
    void setBooleanProperty(String name, boolean value);

    /**
     * Sets a integer property on the message
     *
     * @param name the property name or key
     * @param value the property value
     * @deprecated use {@link #setOutboundProperty(String, Object)} instead
     */
    @Deprecated
    void setIntProperty(String name, int value);

    /**
     * Sets a long property on the message
     *
     * @param name the property name or key
     * @param value the property value
     * @deprecated use {@link #setOutboundProperty(String, Object)} instead
     */
    @Deprecated
    void setLongProperty(String name, long value);

    /**
     * Sets a double property on the message
     *
     * @param name the property name or key
     * @param value the property value
     * @deprecated use {@link #setOutboundProperty(String, Object)} instead
     */
    @Deprecated
    void setDoubleProperty(String name, double value);

    /**
     * Sets a String property on the message
     *
     * @param name the property name or key
     * @param value the property value
     * @deprecated use {@link #setOutboundProperty(String, Object)} instead
     */
    @Deprecated
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
     * Allows for arbitrary data attachments to be associated with the Message. These
     * attachments work in the same way that email attachments work. Attachments can
     * be binary or text
     *
     * @param name the name to associate with the attachment
     * @param dataHandler The attachment datahandler to use. This will be used to
     *            interact with the attachment data.
     * @throws Exception if the attachment cannot be added for any reason
     * @see javax.activation.DataHandler
     * @deprecated use
     *             {@link #addOutboundAttachment(java.lang.String, javax.activation.DataHandler)}
     *             instead
     */
    @Deprecated
    void addAttachment(String name, DataHandler dataHandler) throws Exception;

    /**
     * Allows for arbitrary data attachments to be associated with the Message. These attachments work in the
     * same way that email attachments work. Attachments can be binary or text
     * @param name the name to associate with the attachment
     * @param dataHandler The attachment {@link javax.activation.DataHandler} to use. This will be used to interact with the attachment data
     * @throws Exception if the attachment cannot be added for any reason
     * @see javax.activation.DataHandler
     * @since 3.0
     */
    void addOutboundAttachment(String name, DataHandler dataHandler) throws Exception;

    /**
     *  Adds an outgoing attachment to the message
     * @param object the input stream to the contents of the attachment. This object can either be a {@link java.net.URL}, which will construct a URL data source, or
     * a {@link java.io.File}, which will construct a file data source.  Any other object will be used as the raw contents of the attachment
     * @param contentType the content type of the attachment.  Note that the charset attribute can be specifed too i.e. text/plain;charset=UTF-8
     * @param name the name to associate with the attachments
     * @throws Exception if the attachment cannot be read or created
     * @since 3.0
     */
    void addOutboundAttachment(String name, Object object, String contentType) throws Exception;

    /**
     * Remove an attachment form this message with the specified name
     * @param name the name of the attachment to remove. If the attachment does not exist, the request may be ignored
     * @throws Exception different messaging systems handle attachments differently, as such some will throw an exception
     * if an attachment does dot exist.
     * @deprecated use {@link #removeOutboundAttachment(java.lang.String)} instead
     */
    @Deprecated
    void removeAttachment(String name) throws Exception;

    /**
     * Remove an attachment form this message with the specified name
     * @param name the name of the attachment to remove. If the attachment does not exist, the request may be ignored
     * @throws Exception different messaging systems handle attachments differently, as such some will throw an exception
     * if an attachment does dot exist.
     * @since 3.0
     */
    void removeOutboundAttachment(String name) throws Exception;

    /**
     * Retrieve an attachment with the given name. If the attachment does not exist, null will be returned
     * @param name the name of the attachment to retrieve
     * @return the attachment with the given name or null if the attachment does not exist
     * @see javax.activation.DataHandler
     * @deprecated use {@link #getInboundAttachment(String)} instead
     */
    @Deprecated
    DataHandler getAttachment(String name);

    /**
     * Retrieve an attachment with the given name. If the attachment does not exist, null will be returned
     * @param name the name of the attachment to retrieve
     * @return the attachment with the given name or null if the attachment does not exist
     * @see javax.activation.DataHandler
     * @since 3.0
     */
    DataHandler getInboundAttachment(String name);

    /**
     * Retrieve an attachment with the given name. If the attachment does not exist, null will be returned
     * @param name the name of the attachment to retrieve
     * @return the attachment with the given name or null if the attachment does not exist
     * @see javax.activation.DataHandler
     * @since 3.0
     */
    DataHandler getOutboundAttachment(String name);

    /**
     * @return a set of the names of the attachments on this message. If there are no attachments an empty set will be
     * returned.
     * @deprecated use {@link #getInboundAttachmentNames()}
     */
    @Deprecated
    Set<String> getAttachmentNames();

    /**
     * @return a set of the names of the attachments on this message. If there are no attachments an empty set will be
     * returned.
     * @since 3.0
     */
    Set<String> getInboundAttachmentNames();

    /**
     * @return a set of the names of the attachments on this message. If there are no attachments an empty set will be
     * returned.
     * @since 3.0
     */
    Set<String> getOutboundAttachmentNames();

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
     */
    void release();

    /**
     * Will apply a list of transformers to the payload of the message. This *Will* change the payload of the
     * message. This method provides the only way to alter the paylaod of this message without recreating a
     * copy of the message
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    void applyTransformers(MuleEvent event, List<? extends Transformer> transformers) throws MuleException;

    /**
     * Will apply a list of transformers to the payload of the message. This *Will* change the payload of the
     * message. This method provides the only way to alter the paylaod of this message without recreating a
     * copy of the message
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    void applyTransformers(MuleEvent event, Transformer... transformers) throws MuleException;

    /**
     * Will apply a list of transformers to the payload of the message. This *Will* change the payload of the
     * message. This method provides the only way to alter the paylaod of this message without recreating a
     * copy of the message
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @param outputType the required output type for this transformation. by adding this parameter some additional
     * transformations will occur on the message payload to ensure that the final payload is of the specified type.
     * If no transformers can be found in the registry that can transform from the return type of the transformation
     * list to the outputType and exception will be thrown
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    void applyTransformers(MuleEvent event, List<? extends Transformer> transformers, Class<?> outputType) throws MuleException;

    /**
     * Update the message payload. This is typically only called if the
     * payload was originally an InputStream. In which case, if the InputStream
     * is consumed, it needs to be replaced for future access.
     *
     * @param payload the object to assign as the message payload
     */
    void setPayload(Object payload);

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transformers added to the registry will be checked for compatibility
     * @param outputType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an InputStream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    <T> T getPayload(Class<T> outputType) throws TransformerException;

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transformers added to the registry will be checked for compatability
     * @param outputType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an InputStream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    <T> T getPayload(DataType<T> outputType) throws TransformerException;


    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    String getPayloadAsString(String encoding) throws Exception;

    /**
     * Converts the message implementation into a String representation. If encoding
     * is required it will use the encoding set on the message
     *
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     *
     */
    String getPayloadAsString() throws Exception;

    /**
     * Converts the message implementation into a byte array representation
     *
     * @return byte array of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     *
     */
    byte[] getPayloadAsBytes() throws Exception;

    /**
     * Returns the original payload used to create this message. The payload of the message can change
     * if {@link #applyTransformers(MuleEvent,java.util.List)} or
     * {@link #applyTransformers(MuleEvent, java.util.List, Class)} is called.
     * @return the original payload used to create this message
     */
    Object getOriginalPayload();

    /**
     * Get the message payload for logging without throwing exception
     * Converts the message implementation into a String representation.
     *
     * @return message payload as object
     */
    String getPayloadForLogging();

    /**
     * Get the message payload for logging without throwing exception
     * Converts the message implementation into a String representation. If encoding
     * is required it will use the encoding set on the message
     *
     * @return message payload as object
     */
    String getPayloadForLogging(String encoding);

    /**
     * @deprecated
     * Avoid getting access to the MuleContext through the message.
     * You can get access to the MuleContext by making your class implement {@link org.mule.api.context.MuleContextAware}
     */
    @Deprecated
    MuleContext getMuleContext();

    /**
     * Returns the data type (if any) associated with the message's payload.
     */
    DataType<?> getDataType();

    /**
     * @deprecated use {@link org.mule.api.MuleSession#getProperty(String)}
     */
    @Deprecated
    <T> T getSessionProperty(String name, T defaultValue);

    /**
     * @deprecated use {@link org.mule.api.MuleSession#getProperty(String)}
     */
    @Deprecated
    <T> T getSessionProperty(String name);

    /**
     * @deprecated use {@link org.mule.api.MuleSession#setProperty(String, Serializable)}
     */
    @Deprecated
    void setSessionProperty(String key, Object value);

    /**
     * Copy an inbound message to an outbound one, moving all message properties and attachments
     * @return the inbound message
     */
    MuleMessage createInboundMessage() throws Exception;

    /**
     * Removes all outbound attachments on this message.  Note: inbound attachments are immutable.
     * {@link org.mule.api.transport.PropertyScope#OUTBOUND}.
     */
    void clearAttachments();
}
