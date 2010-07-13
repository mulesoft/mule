/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.routing.filter.Filter;
import org.mule.processor.AbstractFilteringMessageProcessor;

public class MessageFilter extends AbstractFilteringMessageProcessor
{
    protected Filter filter;

    public MessageFilter(Filter filter)
    {
        this.filter = filter;
    }

    @Override
    protected boolean accept(MuleEvent event)
    {
        if (filter == null)
        {
            return true;
        }

        if (event != null)
        {
            return filter.accept(event.getMessage());
        }
        else
        {
            return false;
        }
    }

}
