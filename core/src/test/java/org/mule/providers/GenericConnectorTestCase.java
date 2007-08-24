/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleRuntimeException;
import org.mule.tck.AbstractMuleTestCase;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

/**
 * The test is not there in AbstractConnector, because we need to call a protected
 * method, and the latter class is in a different package.
 */
public class GenericConnectorTestCase extends AbstractMuleTestCase
{

    /**
     * Throwable should not cause a ClassCastException (MULE-802).
     * 
     * @throws Exception
     */
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
