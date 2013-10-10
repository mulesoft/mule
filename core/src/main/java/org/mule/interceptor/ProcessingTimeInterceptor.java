/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.management.stats.ProcessingTime;

/**
 * Calculate and record the processing time for a message processing chain
 */
public class ProcessingTimeInterceptor extends AbstractEnvelopeInterceptor
{
    public ProcessingTimeInterceptor()
    {
        super();
    }

    public ProcessingTimeInterceptor(MessageProcessor next, FlowConstruct fc)
    {
        setListener(next);
        setFlowConstruct(fc);
    }

    @Override
    public MuleEvent before(MuleEvent event) throws MuleException
    {
        return event;
    }

    @Override
    public MuleEvent after(MuleEvent event) throws MuleException
    {
        return event;
    }


    @Override
    public MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException
    {
        if (time != null)
        {
            time.addFlowExecutionBranchTime(startTime);
        }
        return event;
    }
}
