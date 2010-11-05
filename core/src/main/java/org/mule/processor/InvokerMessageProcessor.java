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
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        else if (matchingMethods.size() == 0)
        {
            throw new RuntimeException("No method with name " + methodName + " and "
                                       + argumentExpressions.length + " parameters found in class '"
                                       + object.getClass().getName() + "'.");
        }
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent = event;
        ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();
        Object[] args = new Object[argumentExpressions.length];
        for (int i = 0; i < args.length; i++)
        {
            args[i] = expressionManager.evaluate(argumentExpressions[i], event.getMessage());
            if (!(method.getParameterTypes()[i].isAssignableFrom(args[i].getClass())))
            {
                args[i] = event.getMuleContext().getRegistry().lookupTransformer(args[i].getClass(),
                    method.getParameterTypes()[i]).transform(args[i]);
            }
        }

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resultEvent;
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
