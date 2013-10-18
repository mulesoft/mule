/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.example.geomail.dao.Sender;
import org.mule.example.geomail.dao.SenderDao;

import org.apache.log4j.Logger;

/**
 * TODO
 */
public class Storage implements Callable
{
    private static final Logger log = Logger.getLogger(Storage.class.getName());

    private SenderDao senderDao;

    public Object onCall(MuleEventContext context) throws Exception
    {
        MuleMessage message = context.getMessage();

        log.info("GroupSize: " + message.getCorrelationGroupSize());
        log.info("Correlation ID: " + message.getCorrelationId());
        log.info("Sequence Number: " + message.getCorrelationSequence());
        
//        String ip = message.getOutboundProperty("ip");
//        if (ip == null)
//        {
//            throw new IllegalStateException("'ip' property should have been set on MuleMessage.");
//        }

        String from = message.getOutboundProperty("from.email.address");

        Sender sender = (Sender) message.getPayload();
//        sender.setIp(ip);
        sender.setEmail(from);

        if (getSenderDao().getSender(sender.getIp()) != null)
        {
            log.warn("Sender '" + sender + "' should not be in the Database.");
        }
        else
        {
            getSenderDao().addSender(sender);
            log.warn("Sender '" + sender + "' successfully added to the Database.");
        }

        MuleMessage resultMessage = new DefaultMuleMessage(sender, context.getMuleContext());
        resultMessage.setCorrelationGroupSize(message.getCorrelationGroupSize());
        resultMessage.setCorrelationId(message.getCorrelationId());
        resultMessage.setCorrelationSequence(message.getCorrelationSequence());

        return resultMessage;
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
