/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
