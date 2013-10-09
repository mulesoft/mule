/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionEnricher;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.registry.PreInitProcessor;

/**
 * Registers ExpressionEvaluators with the {@link org.mule.expression.DefaultExpressionManager} so that they will
 * be resolved at run-time.
 * {@link org.mule.api.expression.ExpressionEvaluator} objects are used to execute property expressions (usually on the
 * current message) at run-time to extract a dynamic value.
 */
public class ExpressionEnricherProcessor implements PreInitProcessor, Disposable
{
    private MuleContext context;

    public ExpressionEnricherProcessor(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if(object instanceof ExpressionEnricher)
        {
            context.getExpressionManager().registerEnricher((ExpressionEnricher) object);
        }
        return object;
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    public void dispose()
    {
        context.getExpressionManager().clearEvaluators();
    }
}
