/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.interceptor.Interceptor;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after
 * an event is received.
 */
public abstract class EnvelopeInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor
{
    /**
     * This method is invoked before the event is processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract MuleEvent before(MuleEvent event) throws MuleException;

    /**
     * This method is invoked after the event has been processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract MuleEvent after(MuleEvent event) throws MuleException;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent = before(event);
        resultEvent = processNext(resultEvent);
        resultEvent = after(resultEvent);
        return resultEvent;
    }
}
