/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.components;

import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;

/**
 * TODO
 */
public class Mail implements Callable
{
    public Object onCall(MuleEventContext eventContext) throws Exception {

        MuleMessage message = eventContext.getMessage();

        Message mail = (Message) message.getPayload();

        String from = mail.getFrom()[0].toString();
        String[] received = mail.getHeader("Received");

        List<String> list = new ArrayList<String>();

        for (int i = received.length - 1; i >= 0; i--) 
        {
            ReceivedHeader receivedHeader = ReceivedHeader.getInstance(received[i]);
            if (receivedHeader != null && receivedHeader.getFrom() != null) 
            {
                if (!receivedHeader.getFrom().startsWith("localhost") && !receivedHeader.getFrom().startsWith("127.0.0.1")) 
                {
                    String ip = getFromIP(receivedHeader);

                    if (ip != null) 
                    {
                        list.add(ip);
                    }
                }
            }
        }

        if (list.isEmpty()) 
        {
            throw new DefaultMuleException("Received e-mail does not provide sender IP information.");
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("from.email.address", from);

        MuleMessage result = new DefaultMuleMessage(list, properties, eventContext.getMuleContext());
        return result;
    }

    private String getFromIP(ReceivedHeader receivedHeader) 
    {
        String result = null;

        Matcher matcher = Pattern.compile(".*\\(.*\\[(.*?)\\]\\)", Pattern.DOTALL).matcher(receivedHeader.getFrom());
        if (matcher.matches()) 
        {
            result = matcher.group(1);
        }

        return result;
    }
}
