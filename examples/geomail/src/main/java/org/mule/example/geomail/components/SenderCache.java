/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import org.mule.example.geomail.dao.Sender;
import org.mule.example.geomail.dao.SenderDao;

/**
 * TODO
 */
public class SenderCache
{
    private SenderDao senderDao;

    public Sender storeSender(Sender sender) throws Exception
    {
        if (getSenderDao().getSender(sender.getIp()) == null)
        {
            getSenderDao().addSender(sender);
        }
        return sender;
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
