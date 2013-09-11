/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.routing;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.example.geomail.dao.SenderDao;

/**
 * TODO
 */
public class InCacheFilter implements Filter
{
    private SenderDao senderDao = null;

    public boolean accept(MuleMessage message)
    {
        boolean result = false;

        String ip = (String)message.getPayload();

        // If 'ip' is found in DAO, accept it:
        if (getSenderDao().getSender(ip) != null)
        {
            result = true;
        }

        return result;
    }

    public SenderDao getSenderDao()
    {
        return senderDao;
    }

    public void setSenderDao(SenderDao senderDao)
    {
        this.senderDao = senderDao;
    }
}
