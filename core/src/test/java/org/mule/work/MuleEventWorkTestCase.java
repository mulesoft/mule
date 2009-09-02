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
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Test case to reproduce issue described in MULE-4407 and validate fix.
 */
public class MuleEventWorkTestCase extends AbstractMuleTestCase
{

    protected MuleEvent originalEvent;
    protected Latch latch = new Latch();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // Create a dummy event and give it some properties
        originalEvent = getTestEvent("test");
        originalEvent.getMessage().setProperty("test", "val");
        originalEvent.getMessage().setProperty("test2", "val2");
        OptimizedRequestContext.unsafeSetEvent(originalEvent);
    }

    public void testScheduleMuleEventWork() throws Exception
    {
        muleContext.getWorkManager().scheduleWork(new TestMuleEventWork(originalEvent));

        assertTrue("Timed out waiting for latch", latch.await(2000, TimeUnit.MILLISECONDS));

        assertSame(originalEvent, RequestContext.getEvent());

        try
        {
            // Ensure that even after Work has been created, scheduled and executed
            // the original event instance is still owned by this thread and still
            // mutable.
            ((AbstractMessageAdapter) originalEvent.getMessage().getAdapter()).assertAccess(AbstractMessageAdapter.WRITE);
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    public void testRunMuleEventWork() throws Exception
    {
        new TestMuleEventWork(originalEvent).run();

        // NOTE: This assertion documents/tests current behaviour but does not seem
        // correct.
        // In scenarios where Work implementations are run in the same thread rather
        // than being scheduled then the RequestContext ThreadLocal value is
        // overwritten with a new copy which is not desirable.
        // See: MULE-4409
        assertNotSame(originalEvent, RequestContext.getEvent());

        try
        {
            // Ensure that even after Work has been created, scheduled and executed
            // the original event instance is still owned by this thread and still
            // mutable.
            ((AbstractMessageAdapter) originalEvent.getMessage().getAdapter()).assertAccess(AbstractMessageAdapter.WRITE);
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }

    }

    private class TestMuleEventWork extends AbstractMuleEventWork
    {

        public TestMuleEventWork(MuleEvent event)
        {
            super(event);
        }

        @Override
        protected void doRun()
        {
            assertNotSame("MuleEvent", event, originalEvent);
            assertNotNull("RequestContext.getEvent() is null", RequestContext.getEvent());

            try
            {
                // Ensure that the new event copied for this event is owned by the
                // thread that is executing this work and is mutable
                ((AbstractMessageAdapter) event.getMessage().getAdapter()).assertAccess(AbstractMessageAdapter.WRITE);
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
            latch.countDown();
        }
    }

}
