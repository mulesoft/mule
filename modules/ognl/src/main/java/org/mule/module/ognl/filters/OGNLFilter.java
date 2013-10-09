/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ognl.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;
import ognl.Ognl;
import ognl.OgnlException;

import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationException;
import org.mule.api.routing.filter.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OGNLFilter implements Filter
{
    protected final Log logger = LogFactory.getLog(this.getClass());

    private volatile String expression;
    private volatile Object compiledExpression;

    public String getExpression()
    {
        return expression;
    }

    /**
     * Sets the expression for this filter. The argument must be a valid expression
     * as described in the OGNL documentation.
     *
     * @param expression the expression to use for message evaluation
     * @throws ConfigurationException if the expression cannot be parsed
     * @see Ognl#parseExpression(String)
     */
    public void setExpression(String expression) throws ConfigurationException
    {
        try
        {
            this.compiledExpression = Ognl.parseExpression(expression);
            this.expression = expression;
        }
        catch (OgnlException ex)
        {
            throw new ConfigurationException(ex);
        }
    }

    public boolean accept(MuleMessage message)
    {
        // no message: nothing to filter
        if (message == null)
        {
            return false;
        }

        Object candidate = message.getPayload();
        // no payload: still nothing to filter
        if (candidate == null)
        {
            return false;
        }

        // no expression configured: we reject by default
        if (compiledExpression == null)
        {
            logger.warn("No expression configured - rejecting message.");
            return false;
        }

        try
        {
            Object result = Ognl.getValue(compiledExpression, candidate);
            // we only need to take boolean expressoin results into account
            if (result instanceof Boolean)
            {
                return ((Boolean) result).booleanValue();
            }
        }
        catch (OgnlException ex)
        {
            logger.error(ex);
        }

        // default action: reject
        return false;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final OGNLFilter other = (OGNLFilter) obj;
        return equal(expression, other.expression);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expression});
    }
}
