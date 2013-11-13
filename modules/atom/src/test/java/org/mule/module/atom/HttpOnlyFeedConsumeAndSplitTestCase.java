/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class HttpOnlyFeedConsumeAndSplitTestCase extends FunctionalTestCase
{
    private static final long SLEEP_TIME = 10000;

    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigFile()
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
