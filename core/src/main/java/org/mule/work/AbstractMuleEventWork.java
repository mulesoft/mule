/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;

import javax.resource.spi.work.Work;

/**
 * Abstract implementation of Work to be used whenever Work needs to be scheduled
 * that operates on a MuleEvent. Implementations of AbstractMuleEventWork should be
 * run/scheduled only once.
 * NOTE: This approach does not attempt to resolve MULE-4409 so this work may
 * need to be reverted to correctly fix MULE-4409 in future releases.
 */
public abstract class AbstractMuleEventWork implements Work
{

    protected MuleEvent event;

    public AbstractMuleEventWork(MuleEvent event)
    {
        this.event = event;
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
