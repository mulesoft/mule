/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.util.expression.ExpressionRuntimeException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This transformer will evaluate one or more expressions on the current message and return the results as an Array. If only one
 * expression is defined it will return the object returned from the expression.
 * You can use expressions to extract -
 * - headers (single, map or list)
 * - attachments (single, map or list)
 * - payload
 * - xpath
 * - groovy
 * - bean
 * and more.
 *
 * This transformer provides a very powerful way to pull different bits of information from the message and pass
 * them to the service.
 */
public class ExpressionTransformer extends AbstractMessageAwareTransformer
{
    private List arguments;

    public ExpressionTransformer()
    {
        //No type checking by default
        registerSourceType(Object.class);
        setReturnClass(Object.class);
        arguments = new ArrayList(4);
    }

    public void addArgument(Argument argument)
    {
        arguments.add(argument);
    }

    public boolean removeArgument(Argument argument)
    {
        return arguments.remove(argument);
    }
    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *
     */
    //@Override
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        if(arguments==null || arguments.size()==0)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("arguments[]"), this);
        }

        for (Iterator iterator = arguments.iterator(); iterator.hasNext();)
        {
            Argument argument = (Argument) iterator.next();
            try
            {
                argument.validate();
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        return LifecycleTransitionResult.OK;
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object results[] = new Object[arguments.size()];
        int i =0;
        for (Iterator iterator = arguments.iterator(); iterator.hasNext();i++)
        {
            Argument argument = (Argument) iterator.next();
            try
            {
                results[i] = ExpressionEvaluatorManager.evaluate(argument.getFullExpression(), message);
            }
            catch (ExpressionRuntimeException e)
            {
                throw new TransformerException(this, e);
            }

            if(!argument.isOptional() && results[i]==null)
            {
                throw new TransformerException(CoreMessages.expressionEvaluatorReturnedNull(
                        argument.getEvaluator(), argument.getExpression()), this);

            }

        }
        if(results.length==1)
        {
            return results[0];
        }
        else
        {
            return results;
        }
    }

    public List getArguments()
    {
        return arguments;
    }

    public void setArguments(List arguments)
    {
        this.arguments = arguments;
    }

    public static class Argument
    {
        public static final String EVAL_TOKEN = ":";
        private String expression;
        private String evaluator;
        private String customEvaluator;
        private boolean optional;

        public Argument()
        {
        }

        public String getCustomEvaluator()
        {
            return customEvaluator;
        }

        public void setCustomEvaluator(String customEvaluator)
        {
            this.customEvaluator = customEvaluator;
        }

        public String getEvaluator()
        {
            return evaluator;
        }

        public void setEvaluator(String evaluator)
        {
            this.evaluator = evaluator;
        }

        public String getExpression()
        {
            return expression;
        }

        public void setExpression(String expression)
        {
            this.expression = expression;
        }

        public boolean isOptional()
        {
            return optional;
        }

        public void setOptional(boolean optional)
        {
            this.optional = optional;
        }

        protected String getFullExpression()
        {
            //Sprecial handling of these evaluators since they don't retuen nul if some headers or attachments were found
            if(!optional && ( evaluator.equals("headers") || evaluator.equals("headers-list") ||
               (evaluator.equals("attachments") || evaluator.equals("attachments-list"))))
            {
                return evaluator + EVAL_TOKEN + expression + "required";
            }
            return evaluator + EVAL_TOKEN + expression;
        }

        protected void validate()
        {
            if(expression==null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
            }

            if(evaluator ==null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("evaluator").getMessage());
            }

            if(evaluator.equals("custom"))
            {
                if(customEvaluator==null)
                {
                    throw new IllegalArgumentException(CoreMessages.objectIsNull("customEvaluator").getMessage());
                }
                else
                {
                    evaluator = customEvaluator;
                }
            }

            if(!ExpressionEvaluatorManager.isEvaluatorRegistered(evaluator))
            {
                throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator).getMessage());
            }
        }
    }
}
