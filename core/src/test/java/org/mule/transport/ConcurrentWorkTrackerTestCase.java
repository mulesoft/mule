/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

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

    @Test
    public void cleansUpPendingWorksOnDispose() throws Exception
    {
        Runnable work = mock(Runnable.class);
        workTracker.addWork(work);

        workTracker.dispose();

        assertThat(workTracker.pendingWorks(), not(hasItem(work)));
    }
}
