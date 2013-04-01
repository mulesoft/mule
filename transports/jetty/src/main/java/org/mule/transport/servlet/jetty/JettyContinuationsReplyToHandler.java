/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
