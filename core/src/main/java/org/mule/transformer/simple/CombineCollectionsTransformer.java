/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.processor.MessageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * Takes a payload which is a Collection of Collections and turns into a single List. For example, if the payload is a Collection
 * which contains a Collection with elements A and B and another Collection with elements C and D, this will turn them into
 * a single Collection with elements A, B, C and D.
 *    
 * This transformer will also work on MuleMessageCollections. In this case, it will take the individual Collection
 * payloads of each MuleMessage and merge them into a single Collection on a new MuleMessage.
 */
public class CombineCollectionsTransformer implements MessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleMessage msg = event.getMessage();

        List<Object> payload = new ArrayList<Object>();
        if (msg instanceof MuleMessageCollection)
        {
            MuleMessageCollection collection = (MuleMessageCollection) msg;
            collection.getPayload();
            for (MuleMessage child : collection.getMessagesAsArray())
            {
                Object childPayload = child.getPayload();
                if (childPayload instanceof Collection)
                {
                    payload.addAll((Collection) childPayload);
                }
                else
                {
                    payload.add(childPayload);
                }
            }
        } 
        else if (msg.getPayload() instanceof Collection)
        {
            add(payload, (Collection)msg.getPayload());
        }
        else
        {
            payload.add(msg.getPayload());
        }

        DefaultMuleMessage listMessage = new DefaultMuleMessage(payload, msg, msg.getMuleContext());
        return new DefaultMuleEvent(listMessage, event);
    }

    private void add(List<Object> newPayload, Collection existingPayload)
    {
        for (Object o : existingPayload)
        {
            if (o instanceof Collection)
            {
                newPayload.addAll((Collection)o);
            }
            else 
            {
                newPayload.add(o);
            }
        }
    }
}
