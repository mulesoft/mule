/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.ReplyToHandler;
import org.mule.util.concurrent.Latch;

public class SensingNullReplyToHandler implements ReplyToHandler
{

    public MuleEvent event;
    public Exception exception;
    public Latch latch = new Latch();

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        this.event = event;
        latch.countDown();
    }

    @Override
    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
    {
        this.exception = exception;
        latch.countDown();
    }

    public void clear()
    {
        event = null;
    }
}
