/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.IOUtils;

import java.io.IOException;

public class ExpressionLanguageComponent implements MessageProcessor, MuleContextAware, Initialisable
{

    protected MuleContext muleContext;
    protected String expression;
    protected String expressionFile;

    @Override
    public void initialise() throws InitialisationException
    {
        if (expressionFile != null)
        {
            try
            {
                expression = IOUtils.getResourceAsString(expressionFile, getClass());
            }
            catch (IOException e)
            {
                throw new InitialisationException(e, this);
            }
        }
        else if (expression == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("expression"), this);
        }
    }

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

    public void setExpressionFile(String expressionFile)
    {
        this.expressionFile = expressionFile;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

}
