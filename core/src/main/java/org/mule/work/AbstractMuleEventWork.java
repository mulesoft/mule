/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.work;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.ThreadSafeAccess;

import javax.resource.spi.work.Work;

/**
 * Abstract implementation of Work to be used whenever Work needs to be scheduled
 * that operates on a MuleEvent. The abstract implementation ensures that a copy of
 * MuleEvent is used and that this copy is available in the RequestContext for this
 * new thread. Implementations of AbstractMuleEventWork should be run/scheduled only
 * once.
 * NOTE: This approach does not attempt to resolve MULE-4409 so this work may
 * need to be reverted to correctly fix MULE-4409 in future releases.
 */
public abstract class AbstractMuleEventWork implements Work
{

    protected MuleEvent event;

    public AbstractMuleEventWork(MuleEvent event)
    {
        // Create a copy of event to be used/owned by the new thread that will be
        // created when this Work is run or scheduled.
        // The copy is created here rather than in run() so that ownership issues
        // don't affect the original event instance. See MULE-4407.
        if (event instanceof ThreadSafeAccess)
        {
            this.event = (MuleEvent) ((ThreadSafeAccess) event).newThreadCopy();
        }
        else
        {
            this.event = event;
        }
    }

    public void run()
    {
        // Ensure MuleEvent is set in RequestContext ThreadLocal of the new thread.
        OptimizedRequestContext.unsafeSetEvent(event);
        doRun();
    }

    protected abstract void doRun();

    public void release()
    {
        // no-op
    }

}
