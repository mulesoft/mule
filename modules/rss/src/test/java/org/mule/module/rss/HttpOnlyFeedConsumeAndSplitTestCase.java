/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpOnlyFeedConsumeAndSplitTestCase extends FunctionalTestCase
{

    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigResources()
    {
        return "http-only-consume-and-split.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        FunctionalTestComponent comp = (FunctionalTestComponent) getComponent("feedConsumer");
        comp.setEventCallback(counter);
    }

    @Test
    public void testConsume() throws Exception
    {

        Thread.sleep(4000);
        int count = counter.getCallbackCount();
        assertTrue(count > 0);
        Thread.sleep(3000);
        //We should only receive entries once
        assertEquals(count, counter.getCallbackCount());
    }
}
