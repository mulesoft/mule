/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class MuleBeanExpressionResolverTest extends AbstractMuleTestCase
{

    MuleBeanExpressionResolver muleBeanExpressionResolver = new MuleBeanExpressionResolver();

    @Test
    public void expressionWithNoSuffixIsParsedAsLiteral()
    {
        String expressionWithoutSuffix = "$kasdoawac$@&^#{";
        Object resolvedExpression = muleBeanExpressionResolver.evaluate(expressionWithoutSuffix, null);

        assertThat(resolvedExpression, instanceOf(String.class));
        assertEquals(expressionWithoutSuffix, resolvedExpression.toString());
    }
}