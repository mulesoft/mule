/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
