/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import static org.mule.module.rss.SampleFeed.ENTRIES_IN_RSS_FEED;

public class FileRssFeedConsumeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "file-rss-consume.xml";
    }

    @Test
    public void testConsumeFeedEntries() throws Exception
    {
        createSampleFeedFileInWorkDirectory();

        final EntryReceiver component = (EntryReceiver) getComponent("feedSplitterConsumer");
        Prober prober = new PollingProber(10000, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return component.getCount() == SampleFeed.ENTRIES_IN_RSS_FEED;
            }

            public String describeFailure()
            {
                return String.format("Did not receive %d feed entries (only got %d)",
                    ENTRIES_IN_RSS_FEED, component.getCount());
            }
        });
    }

    private void createSampleFeedFileInWorkDirectory() throws IOException
    {
        String workDirectory = muleContext.getConfiguration().getWorkingDirectory();
        FileOutputStream fos = new FileOutputStream(new File(workDirectory, "sample-feed.rss"));
        String feed = SampleFeed.feedAsString();
        fos.write(feed.getBytes());
        fos.close();
    }

}
