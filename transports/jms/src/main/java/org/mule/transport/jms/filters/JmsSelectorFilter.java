/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.filters;

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

}
