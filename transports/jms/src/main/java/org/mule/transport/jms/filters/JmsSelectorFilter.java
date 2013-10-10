/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

/**
 * <code>JmsSelectorFilter</code> is a wrapper for a JMS Selector. This filter
 * should not be called. Instead the JmsConnector sets the selector on the
 * destination to the expression set on this filer.
 */
public class JmsSelectorFilter implements Filter
{

    private String expression = null;

    public boolean accept(MuleMessage message)
    {
        // If we have received the message the selector has been honoured
        return true;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final JmsSelectorFilter other = (JmsSelectorFilter) obj;
        return equal(expression, other.expression);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expression});
    }
}
