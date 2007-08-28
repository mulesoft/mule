/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class QuartzFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        //We need to reset the counter since we use this component for schema test
        //and legacy config test. Its a bit crude...
        TestComponent.resetCounter();
        return "quartz-functional-test.xml";
    }

    public void testMuleReceiverJob() throws Exception
    {
        CountDownLatch counter = TestComponent.QUARTZ_COUNTER;
        assertEquals(4, counter.getCount());

        // we wait up to 60 seconds here which is WAY too long for three ticks with 1
        // second interval, but it seems that "sometimes" it takes a very long time
        // for Quartz go kick in. Once it starts ticking everything is fine.
        if (!counter.await(60, TimeUnit.SECONDS))
        {
            fail("CountDown timed out: expected 0, value is: " + counter.getCount());
        }
    }

}
