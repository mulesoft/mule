/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * Helper class to deal with sample-feed.rss
 */
public class SampleFeed
{
    public static final int ENTRIES_IN_RSS_FEED = 25;
    private static final String FEED_FILE = "sample-feed.rss";

    public static String feedAsString() throws IOException
    {
        return IOUtils.getResourceAsString(FEED_FILE, SampleFeed.class);
    }

    public static InputStream feedAsStream() throws IOException
    {
        InputStream stream = IOUtils.getResourceAsStream(FEED_FILE, SampleFeed.class);
        assertNotNull(stream);
        return stream;
    }
}
