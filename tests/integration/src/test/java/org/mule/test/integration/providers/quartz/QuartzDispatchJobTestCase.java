/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.quartz;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class QuartzDispatchJobTestCase extends FunctionalTestCase
{
    protected static CountDownLatch countDown;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/quartz/quartz-dispatch.xml";
    }

    public void testMuleClientDispatchJob() throws Exception
    {
        countDown = new CountDownLatch(3);
        new MuleClient().send("vm://quartz.scheduler", "quartz test", null);
        assertTrue(countDown.await(5000, TimeUnit.MILLISECONDS));
    }
}
