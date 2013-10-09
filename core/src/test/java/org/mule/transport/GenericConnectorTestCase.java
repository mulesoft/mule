/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The test is not there in AbstractConnector, because we need to call a protected
 * method, and the latter class is in a different package.
 */
public class GenericConnectorTestCase extends AbstractMuleContextTestCase
{

    /**
     * Throwable should not cause a ClassCastException (MULE-802).
     * 
     * @throws Exception
     */
    @Test
    public void testSpiWorkThrowableHandling() throws Exception
    {
        try
        {
            AbstractConnector connector = getTestConnector();
            connector.handleWorkException(getTestWorkEvent(), "workRejected");
        }
        catch (MuleRuntimeException mrex)
        {
            assertNotNull(mrex);
            assertTrue(mrex.getCause().getClass() == Throwable.class);
            assertEquals("testThrowable", mrex.getCause().getMessage());
        }
    }

    private WorkEvent getTestWorkEvent()
    {
        WorkEvent event = new WorkEvent(this, // source
            WorkEvent.WORK_REJECTED, getTestWork(), new WorkException(new Throwable("testThrowable")));
        return event;
    }

    private Work getTestWork()
    {
        return new Work()
        {
            public void release()
            {
                // noop
            }

            public void run()
            {
                // noop
            }
        };
    }
}
