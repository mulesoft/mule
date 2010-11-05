/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>InvokerMessageProcessor</code> invokes a specified method of an object. An
 * array of argument expressions can be provided to map the message to the method
 * arguments. The method used is determined by the method name along with the number
 * of argument expressions provided. The results of the expression evaluations will
 * automatically be transformed where possible to the method argument type. Multiple
 * methods with the same name and same number of arguments are not supported
 * currently.
 */
public class InvokerMessageProcessor implements MessageProcessor, Initialisable
{
    private Object object;
    private String methodName;
    private String[] argumentExpressions;

    private Method method;

    public void initialise() throws InitialisationException
    {
        List<Method> matchingMethods = new ArrayList<Method>();
        for (Method methodCandidate : object.getClass().getMethods())
        {
            if (methodCandidate.getName().equals(methodName)
                && methodCandidate.getParameterTypes().length == argumentExpressions.length)
                matchingMethods.add(methodCandidate);
        }
        if (matchingMethods.size() == 1)
        {
            method = matchingMethods.get(0);
        }
        else
        {
            throw new InitialisationException(CoreMessages.methodWithNumParamsNotFoundOnObject(methodName,
                argumentExpressions.length, object), this);
        }
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent = event;
        Object[] args = evaluateArguments(event, argumentExpressions);

        try
        {
            Object result = method.invoke(object, args);
            if (!method.getReturnType().equals(void.class))
            {
                resultEvent = createResultEvent(event, result);
            }
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToInvoke(object.toString()), event, e);
        }
        return resultEvent;
    }

    protected Object[] evaluateArguments(MuleEvent event, String[] expressions) throws MessagingException
    {
        ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();
        Object[] args = new Object[expressions.length];
        try
        {
            for (int i = 0; i < args.length; i++)
            {
                Object arg = expressionManager.evaluate(expressions[i], event.getMessage());
                // If expression evaluates to a MuleMessage then use it's payload
                if (arg instanceof MuleMessage)
                {
                    arg = ((MuleMessage) arg).getPayload();
                }
                if (!(method.getParameterTypes()[i].isAssignableFrom(arg.getClass())))
                {
                    DataType<?> source = DataTypeFactory.create(arg.getClass());
                    DataType<?> target = DataTypeFactory.create(method.getParameterTypes()[i]);
                    // Throws TransformerException if no suitable transformer is
                    // found
                    Transformer t = event.getMuleContext().getRegistry().lookupTransformer(source, target);
                    arg = t.transform(arg);
                }
                args[i] = arg;
            }
            return args;
        }
        catch (TransformerException e)
        {
            throw new MessagingException(event, e);
        }
    }

    public void setObject(Object object)
    {
        this.object = object;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    public void setArgumentExpressionsString(String arguments)
    {
        this.argumentExpressions = arguments.split(",");
    }

    public void setArgumentExpressions(String[] argumentExpressions)
    {
        this.argumentExpressions = argumentExpressions;
    }

    protected MuleEvent createResultEvent(MuleEvent event, Object result) throws MuleException
    {
        if (result instanceof MuleMessage)
        {
            return new DefaultMuleEvent((MuleMessage) result, event);
        }
        else if (result != null)
        {
            event.getMessage().applyTransformers(
                event,
                Collections.<Transformer> singletonList(new TransformerTemplate(
                    new TransformerTemplate.OverwitePayloadCallback(result))));
            return event;
        }
        else
        {
            return new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(),
                event.getMuleContext()), event);
        }
    }

}
