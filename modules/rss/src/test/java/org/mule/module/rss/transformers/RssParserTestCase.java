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

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class RssParserTestCase extends AbstractMuleTestCase
{

    private static List<SyndEntry> entries = null;

    @BeforeClass
    public static void setup() throws Exception
    {
        ObjectToRssFeed objectToRssFeed = new ObjectToRssFeed();
        InputStream fileInputStream;
        fileInputStream = new FileInputStream(new File("src/test/resources/rss-test.xml"));
        SyndFeed feed = (SyndFeed) objectToRssFeed.doTransform(fileInputStream, null);
        entries = feed.getEntries();
    }

    @Test
    public void testEntriesSize() throws Exception
    {
        assertThat(entries.size(), is(2));
    }

    @Test
    public void testRSSWithoutNamespaces() throws Exception
    {
        assertValues(entries.get(0));
    }

    @Test
    public void testRSSWithNamespaces() throws Exception
    {
        assertValues(entries.get(1));
    }

    public void assertValues(SyndEntry entry)
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
