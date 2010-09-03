/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class FeedConsumeNoSplitTestCase extends FunctionalTestCase
{
    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigResources()
    {
        return "rss-consume-no-split.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        FunctionalTestComponent comp = (FunctionalTestComponent)getComponent("feedConsumer");
        comp.setEventCallback(counter);
    }

    public void testConsume() throws Exception {

        Thread.sleep(2000);
        int count = counter.getCallbackCount();
        //We just receive the feed, not split
        assertEquals(1, count);

    }
}
