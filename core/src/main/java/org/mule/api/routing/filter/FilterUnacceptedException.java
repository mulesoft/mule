/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
