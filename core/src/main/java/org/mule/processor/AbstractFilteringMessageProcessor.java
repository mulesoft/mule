/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;

/**
 * Abstract {@link InterceptingMessageProcessor} that can be easily be extended and
 * used for filtering message flow through a {@link MessageProcessor} chain. The
 * default behaviour when the filter is not accepted is to return the request event.
 */
public abstract class AbstractFilteringMessageProcessor extends AbstractInterceptingMessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (accept(event))
        {
            return processNext(event);
        }
        else
        {
            return handleUnaccepted(event);
        }
    }

    protected abstract boolean accept(MuleEvent event);

    protected MuleEvent handleUnaccepted(MuleEvent event) throws MuleException
    {
        return event;
    }

}
