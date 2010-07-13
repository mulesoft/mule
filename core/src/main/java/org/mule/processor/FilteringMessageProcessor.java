/*
 * $Id FilteringMessageProcessor$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;

public class FilteringMessageProcessor implements MessageProcessor
{
    private Filter filter;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return (filter != null && !filter.accept(event.getMessage())) ? null : event;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }
}