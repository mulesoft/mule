/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns the message result of invoking the specified globally accessible message
 * processor. If just the message processor name is specified then the current
 * message is used. If a nested expression is specified then this is first evaluated
 * and the result of this evaluation used when invoking the message processor
 * <b>Example:</b>
 * <ul>
 * <li><b>"StringToMyObjext"</b> - invokes the 'StringToMyObjext' transformer using
 * the current message</li>
 * <li><b>"StringToMyObjext:header:one"</b> - invokes the 'StringToMyObjext'
 * transformerusing the value of the outbound header 'one'</li>
 * <li><b>"thatFlow:xpath://my/path"</b> - invokes the 'thatFlow' flow using the
 * result of the xpath evaluation</li>
 * </ul>
 * If no expression is set the MuleMessage itself will be returned.
 * <p/>
 * 
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageProcessorExpressionEvaluator extends AbstractExpressionEvaluator
{
    protected transient Log logger = LogFactory.getLog(MessageProcessorExpressionEvaluator.class);

    public static final String NAME = "process";

    public Object evaluate(String expression, MuleMessage message)
    {
        MuleContext muleContext = message.getMuleContext();

        String processorName;
        String processorArgExpression = null;
        MuleMessage messageToProcess = message;

        if (StringUtils.isBlank(expression))
        {
            return message;
        }

        boolean isNestedExpression = expression.indexOf(':') > 0;
        if (!isNestedExpression)
        {
            processorName = expression;
        }
        else
        {
            processorName = expression.substring(0, expression.indexOf(':'));
            processorArgExpression = expression.substring(expression.indexOf(':') + 1, expression.length());
        }

        if (processorArgExpression != null)
        {
            messageToProcess = evaluateProcessorArgument(message, processorArgExpression);
        }

        MessageProcessor processor = lookupMessageProcessor(processorName, muleContext);

        try
        {
            return processor.process(new DefaultMuleEvent(messageToProcess, RequestContext.getEvent()))
                .getMessage();
        }
        catch (MuleException e)
        {
            throw new ExpressionRuntimeException(
                CoreMessages.createStaticMessage("Exception invoking MessageProcessor '" + processorName
                                                 + "'"), e);
        }
    }

    protected MessageProcessor lookupMessageProcessor(String processorName, MuleContext muleContext)
    {
        Object processor = muleContext.getRegistry().lookupObject(processorName);

        if (!(processor instanceof MessageProcessor))
        {
            throw new ExpressionRuntimeException(CoreMessages.createStaticMessage("No MessageProcessor '"
                                                                                  + processorName
                                                                                  + "' found."));
        }
        return (MessageProcessor) processor;
    }

    protected MuleMessage evaluateProcessorArgument(MuleMessage message, String processorArgExpression)
    {
        Object result = message.getMuleContext().getExpressionManager().evaluate(processorArgExpression,
            message);
        if (result instanceof MuleMessage)
        {
            return (MuleMessage) result;
        }
        else
        {
            return new DefaultMuleMessage(result, message.getMuleContext());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
