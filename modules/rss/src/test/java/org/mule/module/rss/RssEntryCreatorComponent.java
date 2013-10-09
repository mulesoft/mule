/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        List contents = new ArrayList();
        contents.add(content);
        entry.setContents(contents);
        entry.setAuthor("Ross Mason");
        return entry;
    }
}
