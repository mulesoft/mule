/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
