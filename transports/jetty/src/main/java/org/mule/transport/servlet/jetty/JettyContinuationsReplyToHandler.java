/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.transport.DefaultReplyToHandler;

/**
 * This handler is responsible for resuming the continuation for the current request
 */
public class JettyContinuationsReplyToHandler extends DefaultReplyToHandler
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    public JettyContinuationsReplyToHandler(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        ContinuationsReplyTo continuationReplyTo = (ContinuationsReplyTo) replyTo;
        MuleMessage threadSafeMessage = new DefaultMuleMessage(returnMessage);
        continuationReplyTo.setAndResume(threadSafeMessage);
    }
}
