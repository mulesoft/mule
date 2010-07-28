/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleEvent;
import org.mule.api.source.MessageSource;

/**
 * <p>
 * Processes a {@link MuleEvent}'s by invoking the next {@link MessageProcessor} but receiving the reply,
 * which is turn is returned from this MessageProcessor from a seperate {@link MessageSource} rather than
 * using the return value of the <code>next</code> MessageProcessor invocation. Because two seperate channels
 * are used, most implementations will want to implement the concept of a timeout which defines how long a
 * reply should be waited for.
 * 
 * @since 3.0
 */
public interface AsyncReplyMessageProcessor extends InterceptingMessageProcessor
{

    /**
     * @param messageSource the message source that will be used to receive the reply message
     */
    void setReplySource(MessageSource replyMessageSource);
}
