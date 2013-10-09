/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
