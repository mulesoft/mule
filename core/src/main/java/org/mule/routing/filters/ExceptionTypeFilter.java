/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.util.ClassUtils;

/**
 * A filter that accepts messages that have an exception payload. An Exception type
 * can also be set on this filter to allow it to accept Exception messages of a
 * particular Exception class only.
 */
public class ExceptionTypeFilter extends PayloadTypeFilter
{

    public ExceptionTypeFilter()
    {
        super();
    }


    public ExceptionTypeFilter(String expectedType) throws ClassNotFoundException
    {
        this(ClassUtils.loadClass(expectedType, ExceptionTypeFilter.class));
    }

    public ExceptionTypeFilter(Class expectedType)
    {
        super(expectedType);
    }

    /**
     * Check a given message against this filter.
     * 
     * @param message a non null message to filter.
     * @return <code>true</code> if the message matches the filter
     */
    public boolean accept(MuleMessage message)
    {
        ExceptionPayload epl = message.getExceptionPayload();

        if (getExpectedType() == null)
        {
            return epl != null;
        }
        else if (epl != null)
        {
            return getExpectedType().isAssignableFrom(epl.getException().getClass());
        }
        else
        {
            return false;
        }
    }

}
