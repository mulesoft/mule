/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.atom.transformers.ObjectToFeed;
import org.mule.routing.AbstractRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An inbound router that will split a Feed into entries. A filter can be applied to the entries to omit
 * certain entries, the most common use of this would be to filter out entries that have already been read
 * by using the {@link org.mule.module.atom.routing.EntryLastUpdatedFilter} filter.
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
    private ObjectToFeed objectToFeed = new ObjectToFeed();

    public InboundFeedSplitter()
    {
        acceptedContentTypes = new ArrayList<String>();
        acceptedContentTypes.add("application/atom+xml");
    }

    public MuleEvent[] process(MuleEvent muleEvent) throws MessagingException
    {
        try
        {
            Object payload = muleEvent.getMessage().getPayload();
            Feed feed;
            if (payload instanceof Feed)
            {
                feed = (Feed) payload;
            }
            else
            {
                feed = (Feed) objectToFeed.transform(muleEvent.getMessage().getPayload());
            }

            List<MuleEvent> events = new ArrayList<MuleEvent>();
            //MuleEvent[] events = new MuleEvent[feed.getEntries().size()];
            Set<Entry> entries = new TreeSet<Entry>(new EntryComparator());
            entries.addAll(feed.getEntries());
            for (Entry entry : entries)
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
        catch (TransformerException e)
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
            Entry e1 = (Entry) o1;
            Entry e2 = (Entry) o2;
            if (e1.getPublished().before(e2.getPublished()))
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
