/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;

/**
 * TODO MULE-9856 Move to transport specific code
 * 
 * @deprecated use {@link org.mule.runtime.core.api.MuleMessage} whenever possible. This class
 *             should have dissapeared by the time the mule-api is frozen.
 */
@Deprecated
public interface MutableMuleMessage extends MuleMessage, MutableMessageProperties, MutableMessageAttachments
{

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
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     *
     * @param sequence the sequence number or -1 if the sequence is not important
     */
    void setCorrelationSequence(int sequence);

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
     * If an error occurs while processing this message, a ErrorPayload is attached
     * which contains the root exception and Mule error code, plus any other releated
     * info.
     *
     * @param payload The exception payload to attach to this message
     */
    void setExceptionPayload(ExceptionPayload payload);

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
     * Updates the message dataType
     *
     * @param dataType the dataType to assign as the message
     */
    void setDataType(DataType dataType);

    /**
     * Updates the message payload.
     *
     * @param payload the object to assign as the message payload
     * @param dataType payload's dataType. Not null.
     */
    void setPayload(Object payload, DataType<?> dataType);

    /**
     * Copy an inbound message to an outbound one, moving all message properties and attachments
     * @return the inbound message
     */
    MuleMessage createInboundMessage() throws Exception;

    /**
     * TODO MULE-9856 Replace with the builder
     * 
     * Temporary method used to get {@code this} same instance as the new
     * {@link org.mule.runtime.api.message.MuleMessage} API, supporting generics. This is a temporal, transitional
     * method which will not survive the immutability refactor
     */
    @Deprecated
    <PAYLOAD, ATTRIBUTES extends Serializable> org.mule.runtime.api.message.MuleMessage<PAYLOAD, ATTRIBUTES> asNewMessage();
}
