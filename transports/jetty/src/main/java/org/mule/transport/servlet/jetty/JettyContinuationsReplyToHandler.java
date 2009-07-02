/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.transport.DefaultReplyToHandler;

import java.util.List;

import org.mortbay.util.ajax.Continuation;

/**
 * This handler is responsible for resuming the continuation for the current request
 */
public class JettyContinuationsReplyToHandler extends DefaultReplyToHandler
{
    public JettyContinuationsReplyToHandler(List transformers, MuleContext muleContext)
    {
        super(transformers, muleContext);
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        Continuation continuation = (Continuation)replyTo;
        continuation.setObject(returnMessage);
        continuation.resume();
    }
}
