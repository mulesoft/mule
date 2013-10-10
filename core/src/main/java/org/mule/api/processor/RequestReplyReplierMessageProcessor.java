/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.processor;

import org.mule.api.MuleEvent;
import org.mule.api.source.MessageSource;

/**
 * <p>
 * Processes a {@link MuleEvent}'s by invoking the next {@link MessageProcessor} and
 * then rather than returning the result to this processors {@link MessageSource}
 * sending it via a seperate reply {@link MessageProcessor},
 * <p>
 * Some implementations may not use the replyTo messageProcessor but rather use a
 * message property to determine what should be used for processing the async reply
 * 
 * @since 3.0
 */
public interface RequestReplyReplierMessageProcessor extends InterceptingMessageProcessor
{

    /**
     * @param replyMessageProcessor the message processor that will be used to send
     *            the reply message
     */
    void setReplyProcessor(MessageProcessor replyMessageProcessor);
}
