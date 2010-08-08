/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing.filter;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;


public class FilterUnacceptedException extends MessagingException
{
    private static final long serialVersionUID = -1828111078295716525L;
    
    private transient Filter filter;

    /**
     * @deprecated use FilterUnacceptedException(Message, MuleEvent, Filter)
     * @param message the exception message
     */
    @Deprecated
    public FilterUnacceptedException(Message message, MuleMessage muleMessage, Filter filter)
    {
        super(message, muleMessage);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    public FilterUnacceptedException(Message message, MuleEvent event, Filter filter)
    {
        super(message, event);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    /**
     * @deprecated use FilterUnacceptedException(Message, MuleEvent, Filter, Throwable)
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    @Deprecated
    public FilterUnacceptedException(Message message, MuleMessage muleMessage, Filter filter, Throwable cause)
    {
        super(message, muleMessage, cause);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    public FilterUnacceptedException(Message message, MuleEvent event, Filter filter, Throwable cause)
    {
        super(message, event, cause);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    /**
     * @deprecated use FilterUnacceptedException(MuleEvent, Filter, Throwable)
     */
    @Deprecated
    public FilterUnacceptedException(MuleMessage muleMessage, Filter filter, Throwable cause)
    {
        super(muleMessage, cause);
        this.filter = filter;
        addInfo("Filter", (filter == null ? "null" : filter.toString()));
    }

    public FilterUnacceptedException(MuleEvent event, Filter filter, Throwable cause)
    {
        super(event, cause);
        this.filter = filter;
        addInfo("Filter", (filter == null ? "null" : filter.toString()));
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @deprecated use FilterUnacceptedException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    public FilterUnacceptedException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public FilterUnacceptedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    /**
     * @param message the exception message
     * @deprecated use FilterUnacceptedException(Message, MuleEvent)
     */
    @Deprecated
    public FilterUnacceptedException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
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
