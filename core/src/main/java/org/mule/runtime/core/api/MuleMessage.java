/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.DefaultMuleMessage;

import java.io.Serializable;
import java.util.function.Function;

/**
 * @deprecated use org.mule.runtime.core.api.temp.MuleMessage whenever possible. This class should have dissapeared by the time the mule-api is frozen.
 */
@Deprecated
public interface MuleMessage extends org.mule.runtime.api.message.MuleMessage<Object, Serializable>, MessageProperties, MessageAttachments
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
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or -1 if the size is not known
     */
    int getCorrelationGroupSize();

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
     * Gets the encoding for the current message. For potocols that send encoding
     * information with the message, this method should be overriden to expose the
     * transport encoding, otherwise the default encoding in the Mule configuration
     * will be used.
     *
     * @return the encoding for this message. This method must never return null
     */
    String getEncoding();

    /**
     * @deprecated
     * Avoid getting access to the MuleContext through the message.
     * You can get access to the MuleContext by making your class implement {@link org.mule.runtime.core.api.context.MuleContextAware}
     */
    @Deprecated
    MuleContext getMuleContext();

    /**
     * TODO MULE-9856 Replace with the builder
     */
    @Deprecated
    default MuleMessage transform(Function<MutableMuleMessage, MuleMessage> transform)
    {
        MutableMuleMessage copy = new DefaultMuleMessage(this);
        return transform.apply(copy);
    }

}
