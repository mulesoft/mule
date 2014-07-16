/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing.filter;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;


public class FilterUnacceptedException extends MessagingException
{
    private static final long serialVersionUID = -1828111078295716525L;
    
    private transient Filter filter;

    public FilterUnacceptedException(Message message, MuleEvent event, Filter filter)
    {
        super(message, event);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    public FilterUnacceptedException(Message message, MuleEvent event, Filter filter, Throwable cause)
    {
        super(message, event, cause);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    public FilterUnacceptedException(MuleEvent event, Filter filter, Throwable cause)
    {
        super(event, cause);
        this.filter = filter;
        addInfo("Filter", (filter == null ? "null" : filter.toString()));
    }

    public FilterUnacceptedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    public FilterUnacceptedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public Filter getFilter()
    {
        return filter;
    }
}
