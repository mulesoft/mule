/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.module.atom.transformers.ObjectToFeed;
import org.mule.routing.AbstractSplitter;

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
public class FeedSplitter extends AbstractSplitter
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(FeedSplitter.class);

    public static final String FEED_PROPERTY = "feed.object";
    private Filter entryFilter;
    private ObjectToFeed objectToFeed = new ObjectToFeed();

    public FeedSplitter()
    {
        //By default set the filter so that entries are only read once
        entryFilter = new EntryLastUpdatedFilter(null);
    }

    @Override
    protected List<MuleMessage> splitMessage(MuleEvent event) throws MuleException
    {
        List<MuleMessage> messages = new ArrayList<MuleMessage>();        
        if(event.getMessage().getInboundProperty("Content-Length", -1) == 0)
        {
            logger.info("Feed has no content, ignoring");
            return messages;
        }

        try
        {
            Object payload = event.getMessage().getPayload();
            
            Feed feed;
            if (payload instanceof Feed)
            {
                feed = (Feed) payload;
            }
            else
            {
                feed = (Feed) objectToFeed.transform(event.getMessage().getPayload());
            }

            Set<Entry> entries = new TreeSet<Entry>(new EntryComparator());
            entries.addAll(feed.getEntries());
            for (Entry entry : entries)
            {
                MuleMessage m = new DefaultMuleMessage(entry, event.getMuleContext());
                if (entryFilter != null && !entryFilter.accept(m))
                {
                    continue;
                }
                m.setInvocationProperty(FEED_PROPERTY, feed);
                messages.add(m);
            }
            return messages;
        }
        catch (TransformerException e)
        {
            throw new MessagingException(e.getI18nMessage(), event, e, this);
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

    class EntryComparator implements Comparator<Entry>
    {
        public int compare(Entry e1, Entry e2)
        {
            if(e1==null && e2 !=null)
            {
                return -1;
            }
            else if(e1!=null && e2 ==null)
            {
                return 1;
            }
            else if(e1==null && e2 ==null)
            {
                return 0;
            }
            else if (e1.getPublished() !=null && e1.getPublished().before(e2.getPublished()))
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
