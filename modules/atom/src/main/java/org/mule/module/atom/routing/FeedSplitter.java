/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.module.atom.transformers.ObjectToFeed;
import org.mule.processor.AbstractFilteringMessageProcessor;

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
public class FeedSplitter extends AbstractFilteringMessageProcessor
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(FeedSplitter.class);

    public static final String FEED_PROPERTY = "feed.object";
    private Filter entryFilter;
    private List<String> acceptedContentTypes;
    private ObjectToFeed objectToFeed = new ObjectToFeed();

    public FeedSplitter()
    {
        acceptedContentTypes = new ArrayList<String>();
        acceptedContentTypes.add("application/atom+xml");
    }

    @Override
    public MuleEvent process(MuleEvent muleEvent) throws MuleException
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
            Set<Entry> entries = new TreeSet<Entry>(new EntryComparator());
            entries.addAll(feed.getEntries());
            for (Entry entry : entries)
            {
                MuleMessage m = new DefaultMuleMessage(entry, muleEvent.getMuleContext());
                if (entryFilter != null && !entryFilter.accept(m))
                {
                    continue;
                }
                m.setInvocationProperty(FEED_PROPERTY, feed);
                MuleEvent e = new DefaultMuleEvent(m, muleEvent.getEndpoint(), muleEvent.getFlowConstruct(), muleEvent);
                events.add(e);
            }
            for (MuleEvent event : events)
            {
                processNext(event);
            }
        }
        catch (TransformerException e)
        {
            throw new MessagingException(e.getI18nMessage(), muleEvent, e);
        }
        return null;
    }

    @Override
    public boolean accept(MuleEvent muleEvent)
    {
        String contentType = muleEvent.getMessage().getInboundProperty("Content-Type");
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

    class EntryComparator implements Comparator<Entry>
    {
        public int compare(Entry e1, Entry e2)
        {
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
