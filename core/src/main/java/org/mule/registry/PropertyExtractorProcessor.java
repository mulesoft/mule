/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.registry.ObjectProcessor;
import org.mule.util.expression.ExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;

/** 
 * Registers PropertyExtractors with the {@link org.mule.util.expression.ExpressionEvaluatorManager} so that they will
 * be resolved at runtime.
 * {@link org.mule.util.expression.ExpressionEvaluator} objects are used to execute property expressions (usually on the
 * current message) at runtime to extracta dynamic value.
 */
public class PropertyExtractorProcessor implements ObjectProcessor, Disposable
{
    public PropertyExtractorProcessor()
    {
        ExpressionEvaluatorManager.clearEvaluators();
    }

    public Object process(Object object)
    {
        if(object instanceof ExpressionEvaluator)
        {
            ExpressionEvaluatorManager.registerEvaluator((ExpressionEvaluator)object);
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
        ExpressionEvaluatorManager.clearEvaluators();
    }
}
