/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FeedConsumeAndSplitTestCase extends FunctionalTestCase
{
    private static final int ENTRIES_IN_RSS_FEED = 25;

    private final CounterCallback counter = new CounterCallback();
    private SimpleHttpServer httpServer;
    private AtomicInteger pollCount = new AtomicInteger(0);

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "rss-consume-and-split.xml";
   }

    @Override
    protected void doSetUp() throws Exception
    {
        // start the HTTP server before Mule is started
        startHttpServer();

        super.doSetUp();
        addCounterToFeedConsumerComponent();
    }

    private void startHttpServer() throws IOException
    {
        httpServer = new SimpleHttpServer(dynamicPort.getNumber(), new RssFeeder());
        httpServer.start();
    }

    private void addCounterToFeedConsumerComponent() throws Exception
    {
        FunctionalTestComponent comp = (FunctionalTestComponent) getComponent("feedConsumer");
        comp.setEventCallback(counter);
    }

    @After
    public void stopHttpServer()
    {
        if (httpServer != null)
        {
            httpServer.stop();
        }
    }

    @Test
    public void testConsume() throws Exception
    {
        waitForAllEntriesFromSampleFeedToArrive();
        waitForTheNextPoll();

        // We should only receive entries once
        assertEquals(ENTRIES_IN_RSS_FEED, counter.getCallbackCount());
    }

    private void waitForAllEntriesFromSampleFeedToArrive()
    {
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
                    ENTRIES_IN_RSS_FEED, counter.getCallbackCount());
            }
        });
    }

    private void waitForTheNextPoll()
    {
        final int currentPollCount = pollCount.get();
        Prober prober = new PollingProber(2000, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return pollCount.get() > currentPollCount;
            }

            public String describeFailure()
            {
                return "Poll count did not increment in time";
            }
        });
    }

    private class RssFeeder implements Container
    {
        public void handle(Request request, Response response)
        {
            try
            {
                InputStream rssFeed = SampleFeed.feedAsStream();

                OutputStream responseStream = response.getOutputStream();
                IOUtils.copy(rssFeed, responseStream);
                responseStream.close();
            }
            catch (IOException e)
            {
                fail();
            }

            pollCount.incrementAndGet();
        }
    }
}
