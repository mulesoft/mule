/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>JmsSelectorFilter</code> is a wrapper for a JMS Selector. This filter
 * should not be called. Instead the JmsConnector sets the selector on the
 * destionation to the expression set on this filer.
 */
public class JmsSelectorFilter implements UMOFilter
{
    private String expression = null;

    public boolean accept(UMOMessage message)
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

}
