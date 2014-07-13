/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.ThreadSafeAccess;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case to reproduce issue described in MULE-4407 and validate fix.
 */
public class MuleEventWorkTestCase extends AbstractMuleContextTestCase
{

    protected MuleEvent originalEvent;
    protected Latch latch = new Latch();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // Create a dummy event and give it some properties
        originalEvent = getTestEvent("test");
        originalEvent.getMessage().setOutboundProperty("test", "val");
        originalEvent.getMessage().setOutboundProperty("test2", "val2");
        OptimizedRequestContext.unsafeSetEvent(originalEvent);
    }

    @Test
    public void testRunMuleEventWork() throws Exception
    {
        new TestMuleEventWork(originalEvent).run();

        assertSame(originalEvent, RequestContext.getEvent());

        try
        {
            // Ensure that even after Work has been created, scheduled and executed
            // the original event instance is still owned by this thread and still
            // mutable.
            ((DefaultMuleMessage) originalEvent.getMessage()).assertAccess(ThreadSafeAccess.WRITE);
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
            assertSame("MuleEvent", event, originalEvent);
            assertNotNull("RequestContext.getEvent() is null", RequestContext.getEvent());

            try
            {
                // Ensure that the new event copied for this event is owned by the
                // thread that is executing this work and is mutable
                ((DefaultMuleMessage) event.getMessage()).assertAccess(ThreadSafeAccess.WRITE);
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
            latch.countDown();
        }
    }

}
