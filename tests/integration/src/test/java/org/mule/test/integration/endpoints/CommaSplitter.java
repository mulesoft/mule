/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


