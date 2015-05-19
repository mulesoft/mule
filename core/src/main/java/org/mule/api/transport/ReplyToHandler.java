/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo endpointUri is
 * set on the message
 */

public interface ReplyToHandler
{
    void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException;

    /**
     * Processes replyTo in the case an exception occurred.  Not all implementations will implement this if for example
     * they should only send a reply message in the sucess case.
     *
     * @param exception the exception thrown by processing
     * @param replyTo name of the channel that exception message should be sent if relevant
     */
    void processExceptionReplyTo(MessagingException exception, Object replyTo);

}
