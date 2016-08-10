/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.work;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;

import javax.resource.spi.work.Work;

/**
 * Abstract implementation of Work to be used whenever Work needs to be scheduled
 * that operates on a MuleEvent. The abstract implementation ensures that a copy of
 * MuleEvent is used and that this copy is available in the RequestContext for this
 * new thread. Implementations of AbstractMuleEventWork should be run/scheduled only
 * once. NOTE: This approach does not attempt to resolve MULE-4409 so this work may
 * need to be reverted to correctly fix MULE-4409 in future releases.
 */
public abstract class AbstractMuleEventWork implements Work
{

    protected MuleEvent event;

    public AbstractMuleEventWork(MuleEvent event)
    {
        // Event must be copied here rather than once work is executed, so main flow can't mutate the message
        // before work execution
        this(event, true);
    }

    /**
     * Constructor allowing event copying to be disabled.  This is used when a copy has already been made previously
     * e.g.  if the event is queued before being processed asynchronously like with
     * {@link org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor}
     */
    public AbstractMuleEventWork(MuleEvent event, boolean copyEvent)
    {
        this.event = copyEvent ? DefaultMuleEvent.copy(event) : event;
    }

    @Override
    public final void run()
    {
        try
        {
            // Set event in RequestContext now we are in new thread (fresh copy already made in constructor)
            OptimizedRequestContext.unsafeSetEvent(event);
            doRun();
        }
        finally
        {
            RequestContext.clear();
        }
    }

    protected abstract void doRun();

    @Override
    public void release()
    {
        // no-op
    }

    public MuleEvent getEvent()
    {
        return event;
    }
}
