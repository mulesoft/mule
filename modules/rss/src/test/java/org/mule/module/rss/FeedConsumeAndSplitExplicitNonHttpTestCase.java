/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import static org.mule.module.rss.SampleFeed.ENTRIES_IN_RSS_FEED;

import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class FeedConsumeAndSplitExplicitNonHttpTestCase extends FunctionalTestCase
{
    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigFile()
    {
        return "vm-rss-consume-and-explicit-split.xml";
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
        String feed = SampleFeed.feedAsString();
        muleContext.getClient().dispatch("vm://feed.in", feed, null);

        Prober prober = new PollingProber(10000, 100);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return counter.getCallbackCount() == ENTRIES_IN_RSS_FEED;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Did not receive %d feed entries (only got %d)",
                    SampleFeed.ENTRIES_IN_RSS_FEED, counter.getCallbackCount());
            }
        });
    }
}
