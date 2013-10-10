/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.endpoints;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.routing.AbstractSplitter;

import java.util.ArrayList;
import java.util.List;

public class CommaSplitter extends AbstractSplitter
{
    @Override
    protected List<MuleMessage> splitMessage(MuleEvent event) throws MuleException
    {
        ArrayList<MuleMessage> result = new ArrayList<MuleMessage>();
        String[] parts = event.getMessageAsString().split(",");
        for (int i = 0; i < parts.length; ++i)
        {
            result.add(new DefaultMuleMessage(parts[i], muleContext));
        }
        return result;
    }
}


