/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.PropertyScope;
import org.mule.module.rss.transformers.ObjectToRssFeed;
import org.mule.routing.AbstractRouter;

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
 * Will split the feed into entries.  This router also filters out any entries that are older than the last one read
 * The filter can be configured with a date from which to accept feed entries
 */
public class InboundFeedSplitter extends AbstractRouter implements InboundRouter
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(InboundFeedSplitter.class);

    public static final String FEED_PROPERTY = "feed.object";
    private Filter entryFilter;
    private List<String> acceptedContentTypes;
    private ObjectToRssFeed objectToFeed = new ObjectToRssFeed();

    public InboundFeedSplitter()
    {
        acceptedContentTypes = new ArrayList<String>();
        acceptedContentTypes.add("application/rss+xml");
        acceptedContentTypes.add("application/rss");
    }

    public MuleEvent[] process(MuleEvent muleEvent) throws MessagingException
    {
        try
        {
            Object payload = muleEvent.getMessage().getPayload();
            SyndFeed feed;
            if (payload instanceof SyndFeed)
            {
                feed = (SyndFeed) payload;
            }
            else
            {
                feed = (SyndFeed) objectToFeed.transform(muleEvent.getMessage().getPayload());
            }
            Set<SyndEntry> entries = new TreeSet<SyndEntry>(new EntryComparator());
            entries.addAll(feed.getEntries());
            List<MuleEvent> events = new ArrayList<MuleEvent>();

            for (SyndEntry entry : entries)
            {
                MuleMessage m = new DefaultMuleMessage(entry, getMuleContext());
                if (entryFilter != null && !entryFilter.accept(m))
                {
                    continue;
                }
                m.setProperty(FEED_PROPERTY, feed, PropertyScope.INVOCATION);
                MuleEvent e = new DefaultMuleEvent(m, muleEvent.getEndpoint(), muleEvent.getService(), muleEvent);
                events.add(e);

            }

            return events.toArray(new MuleEvent[]{});
        }
        catch (MuleException e)
        {
            throw new MessagingException(e.getI18nMessage(), muleEvent.getMessage(), e);
        }
    }

    public boolean isMatch(MuleEvent muleEvent) throws MessagingException
    {
        String contentType = muleEvent.getMessage().getStringProperty("Content-Type", null);
        if (contentType != null)
        {
            int i = contentType.indexOf(";");
            contentType = (i > -1 ? contentType.substring(0, i) : contentType);
            return acceptedContentTypes.contains(contentType);
        }
        else
        {
            logger.warn("Content-Type header not set, not accepting the message");
            return false;
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

    public List<String> getAcceptedContentTypes()
    {
        return acceptedContentTypes;
    }

    public void setAcceptedContentTypes(List<String> acceptedContentTypes)
    {
        this.acceptedContentTypes = acceptedContentTypes;
    }

    class EntryComparator implements Comparator
    {

        public int compare(Object o1, Object o2)
        {
            SyndEntry e1 = (SyndEntry) o1;
            SyndEntry e2 = (SyndEntry) o2;
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
