/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.module.rss.transformers.ObjectToRssFeed;
import org.mule.routing.AbstractSplitter;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will split the feed into entries. This message processor also filters out any entries that
 * are older than the last one read. The filter can be configured with a date from
 * which to accept feed entries
 */
public class FeedSplitter extends AbstractSplitter
{
    public static final String FEED_PROPERTY = "feed.object";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(FeedSplitter.class);

    private Filter entryFilter;
    private ObjectToRssFeed objectToFeed = new ObjectToRssFeed();

    public FeedSplitter()
    {
        // By default set the filter so that entries are only read once
        entryFilter = new EntryLastUpdatedFilter(null);
    }

    @Override
    protected List<MuleMessage> splitMessage(MuleEvent event) throws MuleException
    {
        List<MuleMessage> messages = new ArrayList<MuleMessage>();

        if (event.getMessage().getInboundProperty("Content-Length", -1) == 0)
        {
            logger.info("Feed has no content, ignoring");
            return messages;
        }

        try
        {
            SyndFeed feed = transformToFeed(event);

            Set<SyndEntry> entries = new TreeSet<SyndEntry>(new EntryComparator());
            entries.addAll(feed.getEntries());

            for (SyndEntry entry : entries)
            {
                MuleMessage entryMessage = new DefaultMuleMessage(entry, event.getMuleContext());
                if ((entryFilter != null) && !entryFilter.accept(entryMessage))
                {
                    continue;
                }

                entryMessage.setInvocationProperty(FEED_PROPERTY, feed);
                messages.add(entryMessage);
            }
            return messages;
        }
        catch (MuleException e)
        {
            throw new MessagingException(e.getI18nMessage(), event, e, this);
        }
    }

    protected SyndFeed transformToFeed(MuleEvent event) throws TransformerException
    {
        Object payload = event.getMessage().getPayload();
        if (payload instanceof SyndFeed)
        {
            return (SyndFeed) payload;
        }
        else
        {
            return (SyndFeed) objectToFeed.transform(payload);
        }
    }

    public Filter getEntryFilter()
    {
        return entryFilter;
    }

    public void setEntryFilter(Filter entryFilter)
    {
        this.entryFilter = entryFilter;
    }

    static class EntryComparator implements Comparator<SyndEntry>
    {
        public int compare(SyndEntry e1, SyndEntry e2)
        {
            if (e1.getPublishedDate().before(e2.getPublishedDate()))
            {
                return -1;
            }
            else if (e1.equals(e2))
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
    }
}
