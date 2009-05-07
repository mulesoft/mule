/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.expression.CustomExpressionEvaluatorTestCase;
import org.mule.tck.FunctionalTestCase;

public class DeclarativeCustomExpressionEvaluatorTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/declarative-custom-expression-evaluator-test.xml";
    }

    public void testCustomEvalutorRegistered()
    {
        CustomExpressionEvaluatorTestCase.FooExpressionEvaluator customEvaluator =
                new CustomExpressionEvaluatorTestCase.FooExpressionEvaluator();
        assertTrue("Custom evaluator has not been registered",
                   muleContext.getExpressionManager().isEvaluatorRegistered(customEvaluator.getName()));
    }
}
