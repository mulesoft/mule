/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.filters;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * @author Holger Hoffstaette
 */

public class OGNLFilter implements UMOFilter
{
    private static final Log LOGGER = LogFactory.getLog(OGNLFilter.class);

    private String expression;

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public boolean accept(UMOMessage message)
    {
        if (message == null)
        {
            return false;
        }

        Object candidate = message.getPayload();
        if (candidate == null)
        {
            return false;
        }

        if (expression == null)
        {
            LOGGER.warn("Expression for OGNLFilter is not set");
            return false;
        }

        try
        {
            Object result = Ognl.getValue(expression, candidate);
            if (result instanceof Boolean)
            {
                return ((Boolean)result).booleanValue();
            }
        }

        catch (OgnlException ex) {
            LOGGER.error("Error evaluating OGNL expression.", ex);
        }

        // default: reject
        return false;
    }

}
