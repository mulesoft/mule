/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.requestreply;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.processor.AbstractInterceptingMessageProcessor;

public class SimpleRequestReplyReplier extends AbstractInterceptingMessageProcessor
    implements RequestReplyReplierMessageProcessor
{

    protected MessageProcessor replyMessageProcessor;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        replyMessageProcessor.process(processNext(event));
        return null;
    }

    public void setReplyProcessor(MessageProcessor replyMessageProcessor)
    {
        this.replyMessageProcessor = replyMessageProcessor;
    }

}
