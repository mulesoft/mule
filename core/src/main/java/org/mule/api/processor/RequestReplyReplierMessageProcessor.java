/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
