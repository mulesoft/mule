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

import org.mule.api.endpoint.EndpointException;
import org.mule.config.i18n.Message;


public class FilterException extends EndpointException
{
    private static final long serialVersionUID = -1828111078295716525L;
    
    private transient Filter filter;

    /**
     * @param message the exception message
     */
    public FilterException(Message message, Filter filter)
    {
        super(message);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public FilterException(Message message, Filter filter, Throwable cause)
    {
        super(message, cause);
        this.filter = filter;
        addInfo("Filter", filter.toString());
    }

    public FilterException(Filter filter, Throwable cause)
    {
        super(cause);
        this.filter = filter;
        addInfo("Filter", (filter == null ? "null" : filter.toString()));
    }

   /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public FilterException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message the exception message
     */
    public FilterException(Message message)
    {
        super(message);
    }

    public Filter getFilter()
    {
        return filter;
    }
}
