/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;

public class ExpressionLanguageExecutor implements MessageProcessor, MuleContextAware
{

    private MuleContext muleContext;
    private String expression;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        muleContext.getExpressionLanguage().evaluate(expression, event);
        return event;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

}
