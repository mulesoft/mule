/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.work;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;

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
        this.event = DefaultMuleEvent.copy(event);
    }

    public final void run()
    {
        // Set event in RequestContext now we are in new thread (fresh copy already made in constructor)
        OptimizedRequestContext.unsafeSetEvent(event);
        doRun();
    }

    protected abstract void doRun();

    public void release()
    {
        // no-op
    }

    public MuleEvent getEvent()
    {
        return event;
    }
}
