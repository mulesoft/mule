/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression.transformers;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.ExpressionConfig;
import org.mule.transformer.types.DataTypeFactory;

/**
 * TODO
 */
public class ExpressionArgument implements MuleContextAware
{
    private ExpressionConfig expressionConfig = new ExpressionConfig();
    private String name;
    private boolean optional;
    private Class<?> returnClass;
    protected ClassLoader expressionEvaluationClassLoader = ExpressionArgument.class.getClassLoader();
    private MuleContext muleContext;

    public ExpressionArgument()
    {
        super();
    }

    public ExpressionArgument(String name, ExpressionConfig expressionConfig, boolean optional)
    {
        this(name, expressionConfig, optional, null);
    }

    public ExpressionArgument(String name, ExpressionConfig expressionConfig, boolean optional, 
        Class<?> returnClass)
    {
        this.expressionConfig = expressionConfig;
        this.name = name;
        this.optional = optional;
        this.returnClass = returnClass;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public ExpressionConfig getExpressionConfig()
    {
        return expressionConfig;
    }

    public void setExpressionConfig(ExpressionConfig expressionConfig)
    {
        this.expressionConfig = expressionConfig;
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
        return expressionConfig.getFullExpression(muleContext.getExpressionManager());
    }

    protected void validate()
    {
        expressionConfig.validate(muleContext.getExpressionManager());
    }

    /**
     * Evaluates this Expression against the passed in Message.  If a returnClass is set on this Expression Argument it
     * will be checked to ensure the Argument returns the correct class type.
     * @param message the message to execute the expression on
     * @return the result of the expression
     * @throws ExpressionRuntimeException if the wrong return type is returned from the expression.
     */
    public Object evaluate(MuleMessage message) throws ExpressionRuntimeException
    {
        Object result = null;

        // MULE-4797 Because there is no way to specify the class-loader that script
        // engines use and because scripts when used for expressions are compiled in
        // runtime rather than at initialization the only way to ensure the correct
        // class-loader to used is to switch it out here. We may want to consider
        // passing the class-loader to the ExpressionManager and only doing this for
        // certain ExpressionEvaluators further in.
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(expressionEvaluationClassLoader);
            result = muleContext.getExpressionManager().evaluate(getExpression(), getEvaluator(), message,
                !isOptional());
        }
        finally
        {
            // Restore original context class-loader
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }

        if (getReturnClass() != null && result != null)
        {
            if (!getReturnClass().isInstance(result))
            {
                //If the return type does not match, lets attempt to transform it before throwing an error
                try
                {
                    Transformer t = muleContext.getRegistry().lookupTransformer(
                        DataTypeFactory.createFromObject(result), DataTypeFactory.create(getReturnClass()));
                    result = t.transform(result);
                }
                catch (TransformerException e)
                {
                    throw new ExpressionRuntimeException(CoreMessages.transformUnexpectedType(result.getClass(),
                    getReturnClass()), e);
                }

            }
//            if(result instanceof Collection && ((Collection)result).size()==0 && !isOptional())
//            {
//                throw new ExpressionRuntimeException(CoreMessages.expressionEvaluatorReturnedNull(this.getEvaluator(), this.getExpression()));
//            }
        }
        return result;
    }

    public String getExpression()
    {
        return expressionConfig.getExpression();
    }

    public void setExpression(String expression)
    {
        expressionConfig.setExpression(expression);
    }

    public String getEvaluator()
    {
        return expressionConfig.getEvaluator();
    }
    
    public void setEvaluator(String evaluator)
    {
        expressionConfig.setEvaluator(evaluator);
    }

    public void setCustomEvaluator(String evaluator)
    {
        expressionConfig.setCustomEvaluator(evaluator);
    }

    public String getCustomEvaluator()
    {
        return expressionConfig.getCustomEvaluator();
    }

    public Class<?> getReturnClass()
    {
        return returnClass;
    }

    public void setReturnDataType(Class<?> returnClass)
    {
        this.returnClass = returnClass;
    }
    
    public void setExpressionEvaluationClassLoader(ClassLoader expressionEvaluationClassLoader)
    {
        this.expressionEvaluationClassLoader = expressionEvaluationClassLoader;
    }

}
