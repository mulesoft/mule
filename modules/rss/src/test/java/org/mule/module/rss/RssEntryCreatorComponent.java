/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

import java.util.ArrayList;
import java.util.List;

//Currently not used
public class RssEntryCreatorComponent
{
    public SyndEntry readFeed(String title, String content) throws Exception
    {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(title);

        List<String> contents = new ArrayList<String>();
        contents.add(content);
        entry.setContents(contents);

        entry.setAuthor("Ross Mason");
        return entry;
    }
}
