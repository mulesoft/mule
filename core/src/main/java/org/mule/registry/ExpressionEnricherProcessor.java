/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * @deprecated as of 3.7.0 since these are only used by {@link org.mule.registry.TransientRegistry} which is also deprecated. Use post processors
 * for currently supported registries instead (i.e: {@link org.mule.config.spring.SpringRegistry})
 */
@Deprecated
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
