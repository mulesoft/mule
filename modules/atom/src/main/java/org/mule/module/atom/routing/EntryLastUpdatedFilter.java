/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.routing;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import java.util.Date;

import org.apache.abdera.model.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will filter entries in an atom feed based on the lasted Edited date, falling back to the
 * published date if the edit date was not set
 */
public class EntryLastUpdatedFilter implements Filter
{
    /**
     * logger used by this class
     */
    private final transient Log logger = LogFactory.getLog(EntryLastUpdatedFilter.class);
    private Date lastUpdate;

    private boolean acceptWithoutUpdateDate = true;

    public EntryLastUpdatedFilter()
    {
    }

    public EntryLastUpdatedFilter(Date lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }

    public boolean accept(MuleMessage message)
    {
        Entry entry = (Entry) message.getPayload();
        Date updated = entry.getEdited();
        if (updated == null)
        {
            updated = entry.getPublished();
            if (updated == null)
            {
                if (isAcceptWithoutUpdateDate())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Entry does not have a last updated or published date set, assuming the feed should be processed");
                    }
                    return true;
                }
                else
                {
                    if (logger.isWarnEnabled())
                    {
                        logger.warn("Entry does not have a last updated or published date set, not consuming the feed because 'acceptWithoutUpdateDate' is false");
                    }
                    return false;
                }
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
