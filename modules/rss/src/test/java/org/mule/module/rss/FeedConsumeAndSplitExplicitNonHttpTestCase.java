/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Test;

import static org.mule.module.rss.SampleFeed.ENTRIES_IN_RSS_FEED;

public class FeedConsumeAndSplitExplicitNonHttpTestCase extends FunctionalTestCase
{
    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigResources()
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
            public boolean isSatisfied()
            {
                return counter.getCallbackCount() == ENTRIES_IN_RSS_FEED;
            }

            public String describeFailure()
            {
                return String.format("Did not receive %d feed entries (only got %d)",
                    SampleFeed.ENTRIES_IN_RSS_FEED, counter.getCallbackCount());
            }
        });
    }

}
