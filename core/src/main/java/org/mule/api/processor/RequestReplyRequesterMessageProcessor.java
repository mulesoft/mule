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
 * Processes a {@link MuleEvent} by invoking the next {@link MessageProcessor} but
 * receiving the reply, which is turn is returned from this MessageProcessor from a
 * seperate {@link MessageSource} rather than using the return value of the
 * <code>next</code> MessageProcessor invocation. Because two seperate channels are
 * used, most implementations will want to implement the concept of a timeout which
 * defines how long a reply should be waited for.
 *
 * @since 3.0
 */
public interface RequestReplyRequesterMessageProcessor 
{
    /**
     * @param replyMessageSource the message source that will be used to receive the reply message
     */
    void setReplySource(MessageSource replyMessageSource);
}
