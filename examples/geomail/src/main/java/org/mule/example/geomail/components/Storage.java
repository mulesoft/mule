/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * TODO
 */
public class Storage implements Callable
{

    private static final Logger log = Logger.getLogger(Storage.class.getName());

    private SenderDao senderDao;

    public Object onCall(MuleEventContext context) throws Exception {

        MuleMessage message = context.getMessage();

        System.err.println("GroupSize: " + message.getCorrelationGroupSize());
        System.err.println("Correlation ID: " + message.getCorrelationId());
        System.err.println("Sequence Number: " + message.getCorrelationSequence());

        for (Iterator it = message.getPropertyNames().iterator(); it.hasNext();) {
            String key = (String) it.next();
            System.err.println(key + " = " + message.getProperty(key));
        }

        String ip = (String) message.getProperty("ip");
        if (ip == null) {
            throw new IllegalStateException("'ip' property should have been set on UMOMessage.");
        }

        String from = (String) message.getProperty("from.email.address");
        /*
        if (from == null) {
            throw new IllegalStateException("'from.email.address' property should have been set on UMOMessage.");
        }*/

        Sender sender = (Sender) message.getPayload();
        sender.setIp(ip);
        sender.setEmail(from);

        if (getSenderDao().getSender(sender.getIp()) != null) {
            log.warn("Sender '" + sender + "' should not be in the Database.");
        } else {
            getSenderDao().addSender(sender);
            log.warn("Sender '" + sender + "' successfully added to the Database.");
        }

        MuleMessage resultMessage = new DefaultMuleMessage(sender, context.getMuleContext());
        resultMessage.setCorrelationGroupSize(message.getCorrelationGroupSize());
        resultMessage.setCorrelationId(message.getCorrelationId());
        resultMessage.setCorrelationSequence(message.getCorrelationSequence());

        return resultMessage;
    }

    public SenderDao getSenderDao() {
        return senderDao;
    }

    public void setSenderDao(SenderDao senderDao) {
        this.senderDao = senderDao;
    }
}
