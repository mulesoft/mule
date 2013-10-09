/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.requestreply;

import org.mule.VoidMuleEvent;
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
        return VoidMuleEvent.getInstance();
    }

    public void setReplyProcessor(MessageProcessor replyMessageProcessor)
    {
        this.replyMessageProcessor = replyMessageProcessor;
    }

}
