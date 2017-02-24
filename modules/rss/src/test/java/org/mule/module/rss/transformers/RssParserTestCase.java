/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.rss.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class RssParserTestCase extends AbstractMuleTestCase
{

    private List<SyndEntry> entries = null;

    @Before
    public void setup() throws Exception
    {
        ObjectToRssFeed objectToRssFeed = new ObjectToRssFeed();
        InputStream fileInputStream;
        fileInputStream = getClass().getResourceAsStream("/rss-test.xml");
        SyndFeed feed = (SyndFeed) objectToRssFeed.doTransform(fileInputStream, null);
        entries = feed.getEntries();
    }

    @Test
    public void testParsedEntries() throws Exception
    {
        assertRSSEntriesWithoutNamespaces(entries.get(0));
        assertRSSEntriesWithNamespaces(entries.get(1));
        assertThat(entries.size(), is(2));
    }

    private void assertRSSEntriesWithNamespaces(SyndEntry entry)
    {
        assertValues(entry);
    }

    private void assertRSSEntriesWithoutNamespaces(SyndEntry entry)
    {
        assertValues(entry);
    }

    private void assertValues(SyndEntry entry)
    {
        List<SyndCategory> categories = entry.getCategories();
        SyndCategory category = categories.get(0);
        SyndCategory category2 = categories.get(1);
        assertThat(entry.getTitle(), is("Title"));
        assertThat(entry.getLink(), is("Link"));
        assertThat(entry.getAuthor(), is("Creator"));
        assertThat(entry.getLink(), is("Link"));
        assertThat(categories.size(), is(2));
        assertThat(category.getName(), is("Category"));
        assertThat(category2.getName(), is("Category2"));
        assertThat(entry.getDescription().getValue(), is("Description"));
    }

}
