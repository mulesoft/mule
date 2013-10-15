/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.launcher.rss;

import java.util.Iterator;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TagType;

import org.apache.commons.lang.StringEscapeUtils;
import org.mule.api.annotations.param.Payload;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class EntryReceiver
{
    @SuppressWarnings("rawtypes")
    public String processFeed(@Payload SyndFeed feed) throws Exception
    {
        StringBuilder feedXml = new StringBuilder();
        feedXml.append("<muleforge-extensions>");

        for (Iterator it = feed.getEntries().iterator(); it.hasNext();)
        {
            SyndEntry entry = (SyndEntry) it.next();
            processItem(feedXml, entry);
        }
        feedXml.append("</muleforge-extensions>");

        return feedXml.toString();
    }

    private void processItem(StringBuilder feedXml, SyndEntry entry)
    {
        feedXml.append("<muleforge-extension>");
        feedXml.append("<title>" + StringEscapeUtils.escapeXml(entry.getTitle()) + "</title>");
        feedXml.append("<link>" + StringEscapeUtils.escapeXml(entry.getLink()) + "</link>");
        if (!entry.getEnclosures().isEmpty())
        {
            feedXml.append("<image-url>"
                           + StringEscapeUtils.escapeXml(((SyndEnclosure) entry.getEnclosures().get(0)).getUrl())
                           + "</image-url>");
        }
        parseDescription(feedXml, entry);
        feedXml.append("</muleforge-extension>");
    }

    private void parseDescription(StringBuilder feedXml, SyndEntry entry)
    {
        Source source = new Source(entry.getDescription().getValue());

        List<Element> anchors = source.getAllElements(HTMLElementName.A);
        if (anchors != null)
        {
            for (Element a : anchors)
            {
                if ("documentation-url".equalsIgnoreCase(a.getAttributeValue("id")))
                {
                    feedXml.append("<documentation-url>"
                                   + StringEscapeUtils.escapeXml(a.getAttributeValue("href"))
                                   + "</documentation-url>");
                }
                else if ("source-url".equalsIgnoreCase(a.getAttributeValue("id")))
                {
                    feedXml.append("<source-url>" + StringEscapeUtils.escapeXml(a.getAttributeValue("href"))
                                   + "</source-url>");
                }
                else if ("download-url".equalsIgnoreCase(a.getAttributeValue("id")))
                {
                    feedXml.append("<download-url>"
                                   + StringEscapeUtils.escapeXml(a.getAttributeValue("href"))
                                   + "</download-url>");
                }
            }
        }
    }
}
