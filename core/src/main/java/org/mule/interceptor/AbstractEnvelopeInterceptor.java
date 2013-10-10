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
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.interceptor.Interceptor;
import org.mule.management.stats.ProcessingTime;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after
 * an event is received.
 */
public abstract class AbstractEnvelopeInterceptor extends AbstractInterceptingMessageProcessor
                                                  implements Interceptor, FlowConstructAware
{

    protected FlowConstruct flowConstruct;

    /**
     * This method is invoked before the event is processed
     */
    public abstract MuleEvent before(MuleEvent event) throws MuleException;

    /**
     * This method is invoked after the event has been processed, unless an exception was thrown
     */
    public abstract MuleEvent after(MuleEvent event) throws MuleException;

    /**
     *  This method is always invoked after the event has been processed,
     */
    public abstract MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        boolean exceptionWasThrown = true;
        long startTime = System.currentTimeMillis();
        ProcessingTime time = event.getProcessingTime();
        MuleEvent resultEvent = event;
        try
        {
            resultEvent = before(event);
            resultEvent = processNext(resultEvent);
            resultEvent = after(resultEvent);
            exceptionWasThrown = false;
        }
        finally
        {
            resultEvent = last(resultEvent, time, startTime, exceptionWasThrown);
        }
        return resultEvent;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
