/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessageCollection;
import org.mule.module.rss.routing.FeedSplitter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class FeedSplitterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testConsume() throws Exception
    {
        FeedSplitter feedSplitter = new FeedSplitter();
        feedSplitter.setMuleContext(muleContext);

        String feeds = SampleFeed.feedAsString();

        assertThat(routeEventAndCountMessages(feedSplitter, feeds), is(SampleFeed.ENTRIES_IN_RSS_FEED));

        // The second event should not add new messages
        assertThat(routeEventAndCountMessages(feedSplitter, feeds), is(0));
    }

    private int routeEventAndCountMessages(FeedSplitter feedSplitter, Object data) throws Exception
    {
        MuleEvent event = feedSplitter.process(getTestEvent(data));
        if (event instanceof VoidMuleEvent)
        {
            return 0;
        }
        return ((MuleMessageCollection) event.getMessage()).size();
    }
}
