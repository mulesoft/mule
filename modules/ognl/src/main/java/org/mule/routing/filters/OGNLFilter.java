/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.config.ConfigurationException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OGNLFilter implements UMOFilter
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
     * as described ini the OGNL documentation.
     *
     * @param expression the expression to use for message evaluation
     * @throws ConfigurationException if the expression cannot be parsed
     * @see {@link Ognl#parseExpression(String)}
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

    public boolean accept(UMOMessage message)
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

}
