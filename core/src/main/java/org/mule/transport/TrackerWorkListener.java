/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
