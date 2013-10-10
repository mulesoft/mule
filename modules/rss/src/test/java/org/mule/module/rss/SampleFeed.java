/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
