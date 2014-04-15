/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.mvel2.ImmutableElementException;
import org.mule.mvel2.PropertyAccessException;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class AbstractELTestCase extends AbstractMuleContextTestCase
{

    protected Variant variant;
    protected ExpressionLanguage expressionLanguage;

    public AbstractELTestCase(Variant variant, String mvelOptimizer)
    {
        this.variant = variant;
        if (mvelOptimizer != null)
        {
            OptimizerFactory.setDefaultOptimizer(mvelOptimizer);
        }
        else
        {
            OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
        }
    }

    @Before
    public void setupExprssionEvaluator() throws Exception
    {
        expressionLanguage = getExpressionLanguage();
        if (expressionLanguage instanceof Initialisable)
        {
            ((Initialisable) expressionLanguage).initialise();
        }
    }

    @SuppressWarnings("deprecation")
    protected Object evaluate(String expression)
    {
        switch (variant)
        {
            case EVALUATOR_LANGUAGE :
                return expressionLanguage.evaluate(expression);
            case EXPRESSION_MANAGER :
                return muleContext.getExpressionManager().evaluate(expression, (MuleMessage) null);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    protected Object evaluate(String expression, MuleMessage message)
    {
        switch (variant)
        {
            case EVALUATOR_LANGUAGE :
                return expressionLanguage.evaluate(expression, message);
            case EXPRESSION_MANAGER :
                return muleContext.getExpressionManager().evaluate(expression, message);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    protected Object evaluate(String expression, MuleEvent event)
    {
        switch (variant)
        {
            case EVALUATOR_LANGUAGE :
                return expressionLanguage.evaluate(expression, event);
            case EXPRESSION_MANAGER :
                return muleContext.getExpressionManager().evaluate(expression, event);
        }
        return null;
    }

    public static enum Variant
    {
        EXPRESSION_MANAGER, EVALUATOR_LANGUAGE
    }

    @Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{Variant.EVALUATOR_LANGUAGE, null},
            {Variant.EVALUATOR_LANGUAGE, OptimizerFactory.SAFE_REFLECTIVE},
            {Variant.EVALUATOR_LANGUAGE, "ASM"}, {Variant.EXPRESSION_MANAGER, null}});
    }

    protected ExpressionLanguage getExpressionLanguage() throws Exception
    {
        return new MVELExpressionLanguage(muleContext);
    }

    protected void assertUnsupportedOperation(String expression)
    {
        try
        {
            evaluate(expression);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertUnsupportedOperation(String expression, MuleMessage message)
    {
        try
        {
            evaluate(expression, message);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(UnsupportedOperationException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertImmutableVariable(String expression)
    {
        try
        {
            evaluate(expression);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(ImmutableElementException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertImmutableVariable(String expression, MuleMessage message)
    {
        try
        {
            evaluate(expression, message);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(ImmutableElementException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertImmutableVariable(String expression, MuleEvent event)
    {
        try
        {
            evaluate(expression, event);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(ImmutableElementException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertFinalProperty(String expression)
    {
        try
        {
            evaluate(expression);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertFinalProperty(String expression, MuleMessage message)
    {
        try
        {
            evaluate(expression, message);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

    protected void assertFinalProperty(String expression, MuleEvent event)
    {
        try
        {
            evaluate(expression, event);
            fail("ExpressionRuntimeException expected");
        }
        catch (ExpressionRuntimeException e)
        {
            assertEquals(PropertyAccessException.class, ExceptionUtils.getRootCause(e).getClass());
        }
    }

}
