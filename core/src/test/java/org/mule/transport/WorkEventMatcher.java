/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.mockito.Matchers.argThat;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Matches a {@link WorkEvent} instances
 */
public class WorkEventMatcher extends TypeSafeMatcher<WorkEvent>
{

    private Object source;
    private final Work work;
    private final int type;
    private final WorkException workException;
    private final long startDuration;

    public WorkEventMatcher(Object source, Work work, int type, WorkException workException, long startDuration)
    {
        this.source = source;
        this.work = work;
        this.type = type;
        this.workException = workException;
        this.startDuration = startDuration;
    }

    @Override
    public boolean matchesSafely(WorkEvent workEvent)
    {
        return workEvent.getSource() == source && workEvent.getWork() == work
               && workEvent.getException() == workException && workEvent.getStartDuration() == startDuration
               && workEvent.getType() == type;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText(String.format("a WorkEvent for work '%s', type '%s', source '%s', exception '%s' and startDuration '%s'", work, type, source, workException, startDuration));
    }

    public static WorkEvent anAcceptedWorkEventFor(Object source, Work work, WorkException workException, long startDuration)
    {
        return argThat(new WorkEventMatcher(source, work, WorkEvent.WORK_ACCEPTED, workException, startDuration));
    }

    public static WorkEvent aCompletedWorkEventFor(Object source, Work work, WorkException workException, long startDuration)
    {
        return argThat(new WorkEventMatcher(source, work, WorkEvent.WORK_COMPLETED, workException, startDuration));
    }

    public static WorkEvent aRejectedWorkEventFor(Object source, Work work, WorkException workException, long startDuration)
    {
        return argThat(new WorkEventMatcher(source, work, WorkEvent.WORK_REJECTED, workException, startDuration));
    }

    public static WorkEvent aStartedWorkEventFor(Object source, Work work, WorkException workException, long startDuration)
    {
        return argThat(new WorkEventMatcher(source, work, WorkEvent.WORK_STARTED, workException, startDuration));
    }
}