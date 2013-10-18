/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.transport.WorkEventMatcher.aRejectedWorkEventFor;
import static org.mule.transport.WorkEventMatcher.aStartedWorkEventFor;
import static org.mule.transport.WorkEventMatcher.anAcceptedWorkEventFor;
import static org.mule.transport.WorkEventMatcher.aCompletedWorkEventFor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

import org.junit.Test;

@SmallTest
public class TrackerWorkListenerTestCase extends AbstractMuleTestCase
{

    public static final int START_DURATION = 10;

    private final Work originalWork = mock(Work.class);
    private final Work work = mock(Work.class);
    private final WorkException workException = new WorkException();
    private final WorkListener delegate = mock(WorkListener.class);
    private final TrackerWorkListener trackerWorkListener = new TrackerWorkListener(work, delegate);

    @Test
    public void notifiesWorkAccepted()
    {
        WorkEvent workEvent = createWorkEvent(WorkEvent.WORK_ACCEPTED);

        trackerWorkListener.workAccepted(workEvent);

        verify(delegate).workAccepted(anAcceptedWorkEventFor(this, work, workException, START_DURATION));
    }

    @Test
    public void notifiesWorkCompleted()
    {
        WorkEvent workEvent = createWorkEvent(WorkEvent.WORK_COMPLETED);

        trackerWorkListener.workCompleted(workEvent);

        verify(delegate).workCompleted(aCompletedWorkEventFor(this, work, workException, START_DURATION));
    }

    @Test
    public void notifiesWorkRejected()
    {
        WorkEvent workEvent = createWorkEvent(WorkEvent.WORK_REJECTED);

        trackerWorkListener.workRejected(workEvent);

        verify(delegate).workRejected(aRejectedWorkEventFor(this, work, workException, START_DURATION));
    }

    @Test
    public void notifiesWorkStarted()
    {
        int workStarted = WorkEvent.WORK_STARTED;

        trackerWorkListener.workStarted(createWorkEvent(workStarted));

        verify(delegate).workStarted(aStartedWorkEventFor(this, work, workException, START_DURATION));
    }

    private WorkEvent createWorkEvent(int type)
    {
        return new WorkEvent(this, type, originalWork, workException, START_DURATION);
    }
}
