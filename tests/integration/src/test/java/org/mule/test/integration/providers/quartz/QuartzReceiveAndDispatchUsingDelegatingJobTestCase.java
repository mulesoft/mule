/*
 * $Id: QuartzFunctionalTestCase.java 8077 2007-08-27 20:15:25Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.quartz;

import org.mule.extras.client.MuleClient;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.providers.quartz.jobs.MuleClientReceiveJob;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class QuartzReceiveAndDispatchUsingDelegatingJobTestCase extends FunctionalTestCase
{
    protected static CountDownLatch countDown;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/quartz/quartz-receive-dispatch-delegating-job.xml";
    }

    public void testMuleClientReceiveAndDispatchUsingDelegatingJobAsPayload() throws Exception
    {
        countDown = new CountDownLatch(3);

        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);

        Map props = new HashMap();
        props.put(QuartzConnector.PROPERTY_JOB_RECEIVE_ENDPOINT, "vm://event.queue");
        props.put(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT, "vm://quartz.in");
        Object payload = new MuleClientReceiveJob();
        new MuleClient().send("vm://quartz.scheduler", payload, props);
        assertTrue(countDown.await(5000, TimeUnit.MILLISECONDS));
    }

    public void testMuleClientReceiveAndDispatchUsingDelegatingJobAsProperty() throws Exception
    {
        countDown = new CountDownLatch(3);

        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);

        Map props = new HashMap();
        props.put(QuartzConnector.PROPERTY_JOB_RECEIVE_ENDPOINT, "vm://event.queue");
        props.put(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT, "vm://quartz.in");
        props.put(QuartzConnector.PROPERTY_JOB_OBJECT, new MuleClientReceiveJob());

        new MuleClient().send("vm://quartz.scheduler", "test", props);
        assertTrue(countDown.await(5000, TimeUnit.MILLISECONDS));
    }

    public void testMuleClientReceiveAndDispatchUsingDelegatingJobAsPropertyRef() throws Exception
    {
        countDown = new CountDownLatch(3);

        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);
        new MuleClient().send("vm://event.queue", "quartz test", null);

        Map props = new HashMap();
        props.put(QuartzConnector.PROPERTY_JOB_RECEIVE_ENDPOINT, "vm://event.queue");
        props.put(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT, "vm://quartz.in");
        // The ref will be loaded from the default classloader container context, but
        // his
        // could be Jndi or spring
        props.put(QuartzConnector.PROPERTY_JOB_REF, MuleClientReceiveJob.class.getName());

        new MuleClient().send("vm://quartz.scheduler", "test", props);
        assertTrue(countDown.await(5000, TimeUnit.MILLISECONDS));
    }
}
