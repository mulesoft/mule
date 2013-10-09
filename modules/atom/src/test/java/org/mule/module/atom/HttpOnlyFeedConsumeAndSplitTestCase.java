/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom;

import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpOnlyFeedConsumeAndSplitTestCase extends FunctionalTestCase
{

    private static final long SLEEP_TIME = 10000;

    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigResources()
    {
        return "http-only-consume-and-split.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        FunctionalTestComponent comp = (FunctionalTestComponent)getComponent("feedConsumer");
        comp.setEventCallback(counter);
    }

    @Test
    public void testConsume() throws Exception
    {
        // wait until the polling kicks in
        Thread.sleep(SLEEP_TIME);

        int count = counter.getCallbackCount();
        assertTrue("did not receive any artices from feed", count > 0);

        // wait a bit more for the connector to poll again
        Thread.sleep(SLEEP_TIME);
        //We should only receive entries once
        assertEquals(count, counter.getCallbackCount());
    }

}
