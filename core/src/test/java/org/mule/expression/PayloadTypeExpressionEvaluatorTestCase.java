/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.tck.size.SmallTest;

@SmallTest
public class PayloadTypeExpressionEvaluatorTestCase extends AbstractIllegalExpressionEvaluatorTestCase
{

    @Override
    protected IllegalExpressionEvaluator createIllegalExpressionEvaluator()
    {
        return new PayloadTypeExpressionEvaluator();
    }
}
