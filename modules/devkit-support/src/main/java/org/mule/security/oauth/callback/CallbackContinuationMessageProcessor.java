/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.callback;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

/**
 * This {@link MessageProcessor} acts as a wrapper of another one in order to insert
 * it into a chain that it's not its own without being subject to its lifecycle. For
 * example, suppose that the {{@link #continuation} message processor is part of
 * {@link MessageProcessorChain} A. Then we want to insert {@link #continuation} into
 * another chain B. We don't want B to reinitialize {@link #continuation} again, we
 * just want to insert it. This class makes that possible
 */
public class CallbackContinuationMessageProcessor implements MessageProcessor
{

    private MessageProcessor continuation;

    protected CallbackContinuationMessageProcessor(MessageProcessor continuation)
    {
        this.continuation = continuation;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return this.continuation.process(event);
    }

}
