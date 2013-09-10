/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.routing;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;

import java.util.Date;

import org.apache.abdera.model.Feed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will filter a feed who's update date has changed since the last time it was read.  Some feeds to no update
 * this value so {@link #setAcceptWithoutUpdateDate(boolean)} can be set to always consume the feed
 */
public class FeedLastUpdatedFilter implements Filter
{
    /**
     * logger used by this class
     */
    private final transient Log logger = LogFactory.getLog(FeedLastUpdatedFilter.class);
    private Date lastUpdate;

    private boolean acceptWithoutUpdateDate = true;

    public FeedLastUpdatedFilter()
    {
        //For Spring Xml
    }

    public FeedLastUpdatedFilter(Date lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }

    public boolean accept(MuleMessage message)
    {
        Feed feed;
        try
        {
            feed = message.getPayload(Feed.class);
        }
        catch (TransformerException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), e);
        }

        Date updated = feed.getUpdated();
        if (updated == null)
        {
            if (isAcceptWithoutUpdateDate())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Feed does not have a last updated or published date set, assuming the feed should be processed");
                }
                return true;
            }
            else
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn("Feed does not have a last updated or published date set, not consuming the feed because 'acceptWithoutUpdateDate' is false");
                }
                return false;
            }
        }

        if (lastUpdate != null)
        {
            if (lastUpdate.after(updated) || lastUpdate.equals(updated))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Feed update is not newer than the last update, not processing");
                }
                return false;
            }
        }
        lastUpdate = updated;
        return true;

    }

    public boolean isAcceptWithoutUpdateDate()
    {
        return acceptWithoutUpdateDate;
    }

    public void setAcceptWithoutUpdateDate(boolean acceptWithoutUpdateDate)
    {
        this.acceptWithoutUpdateDate = acceptWithoutUpdateDate;
    }

    public Date getLastUpdate()
    {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }
}
