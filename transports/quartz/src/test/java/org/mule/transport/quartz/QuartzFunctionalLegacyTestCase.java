/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent2;
import org.mule.tck.functional.CountdownCallback;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class QuartzFunctionalLegacyTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "quartz-functional-legacy-test.xml";
    }

    public void testMuleReceiverJob() throws Exception
    {
        FunctionalTestComponent2 component = (FunctionalTestComponent2) getComponent("quartzService1");
        assertNotNull(component);
        CountdownCallback count1 = new CountdownCallback(4);
        component.setEventCallback(count1);

        component = (FunctionalTestComponent2) getComponent("quartzService2");
        assertNotNull(component);
        CountdownCallback count2 = new CountdownCallback(2);
        component.setEventCallback(count2);


        // we wait up to 60 seconds here which is WAY too long for three ticks with 1
        // second interval, but it seems that "sometimes" it takes a very long time
        // for Quartz go kick in. Once it starts ticking everything is fine.
        assertTrue("Count 1 timed out: expected 0, value is: " + count1.getCount(), count1.await(60000));
        assertTrue("Count 2 timed out: expected 0, value is: " + count2.getCount(), count2.await(5000));
    }

}
