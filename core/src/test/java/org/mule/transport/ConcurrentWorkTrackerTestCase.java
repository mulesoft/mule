/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ConcurrentWorkTrackerTestCase extends AbstractMuleTestCase
{

    private ConcurrentWorkTracker workTracker = new ConcurrentWorkTracker();

    @Test
    public void addsWork()
    {
        Runnable work = mock(Runnable.class);

        workTracker.addWork(work);

        assertThat(workTracker.pendingWorks(), hasItem(work));
    }

    @Test
    public void removesWork()
    {
        Runnable work = mock(Runnable.class);

        workTracker.addWork(work);
        workTracker.removeWork(work);

        assertThat(workTracker.pendingWorks(), not(hasItem(work)));
    }
}
