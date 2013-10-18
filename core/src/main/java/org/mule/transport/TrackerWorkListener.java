/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * Wraps a {@link WorkListener} associated with a given {@link Work} to delegate
 * notifications originated in a work's wrapper.
 */
public class TrackerWorkListener implements WorkListener
{

    private final Work work;
    private final WorkListener delegate;

    public TrackerWorkListener(Work work, WorkListener delegate)
    {
        this.work = work;
        this.delegate = delegate;
    }

    @Override
    public void workAccepted(WorkEvent e)
    {
        delegate.workAccepted(unwrapWorkEvent(e));
    }

    @Override
    public void workRejected(WorkEvent e)
    {
        delegate.workRejected(unwrapWorkEvent(e));
    }

    @Override
    public void workStarted(WorkEvent e)
    {
        delegate.workStarted(unwrapWorkEvent(e));
    }

    @Override
    public void workCompleted(WorkEvent e)
    {
        delegate.workCompleted(unwrapWorkEvent(e));
    }

    private WorkEvent unwrapWorkEvent(WorkEvent e)
    {
        return new WorkEvent(e.getSource(), e.getType(), work, e.getException(), e.getStartDuration());
    }
}
