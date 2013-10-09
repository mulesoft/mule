/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
