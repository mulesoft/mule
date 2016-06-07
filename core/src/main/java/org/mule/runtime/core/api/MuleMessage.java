/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;
import java.util.Set;

import javax.activation.DataHandler;

/**
 * @deprecated use org.mule.runtime.core.api.temp.MuleMessage whenever possible. This class should have dissapeared by the time the mule-api is frozen.
 */
@Deprecated
public interface MuleMessage extends org.mule.runtime.api.message.MuleMessage<Object, Serializable>, MutableMessageProperties
{

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
     * Allows for arbitrary data attachments to be associated with the Message. These attachments work in the
     * same way that email attachments work. Attachments can be binary or text
     *
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
     *
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
     * @since 3.0
     */
    void removeOutboundAttachment(String name) throws Exception;

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
     * Updates the message payload. This is typically only called if the
     * payload was originally an InputStream. In which case, if the InputStream
     * is consumed, it needs to be replaced for future access.
     *
     * @param payload the object to assign as the message payload
     */
    void setPayload(Object payload);

    /**
     * Updates the message payload.
     *
     * @param payload the object to assign as the message payload
     * @param dataType payload's dataType. Not null.
     */
    void setPayload(Object payload, DataType<?> dataType);

    /**
     * Returns the original payload used to create this message.
     * @return the original payload used to create this message
     */
    Object getOriginalPayload();

    /**
     * @deprecated
     * Avoid getting access to the MuleContext through the message.
     * You can get access to the MuleContext by making your class implement {@link org.mule.runtime.core.api.context.MuleContextAware}
     */
    @Deprecated
    MuleContext getMuleContext();

    /**
     * Copy an inbound message to an outbound one, moving all message properties and attachments
     * @return the inbound message
     */
    MuleMessage createInboundMessage() throws Exception;

    /**
     * Removes all outbound attachments on this message.  Note: inbound attachments are immutable.
     * {@link org.mule.runtime.core.PropertyScope#OUTBOUND}.
     */
    void clearAttachments();

    /**
     * Temporary method used to get {@code this} same instance as the new {@link org.mule.runtime.api.message.MuleMessage} API,
     * supporting generics. This is a temporal, transitional method which will not
     * survive the immutability refactor
     */
    @Deprecated
    <Payload, Attributes extends Serializable> org.mule.runtime.api.message.MuleMessage<Payload, Attributes> asNewMessage();
}
