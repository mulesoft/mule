/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class MuleBeanExpressionResolverTest extends AbstractMuleTestCase
{

    MuleBeanExpressionResolver muleBeanExpressionResolver = new MuleBeanExpressionResolver();

    @Test
    public void expressionWithNoSuffixIsParsedAsLiteral()
    {
        Object resolvedExpression = resolveExpression("$kasdoawac$@&^#{");
        assertThat(resolvedExpression, instanceOf(String.class));
        assertThat(resolvedExpression.toString(), equalTo("$kasdoawac$@&^#{"));
    }

    @Test
    public void nullExpressionSuccessfullyResolved()
    {
        Object resolvedExpression = resolveExpression(null);
        assertThat(resolvedExpression, is(nullValue()));
    }

    protected Object resolveExpression(String expressionWithoutSuffix)
    {
        Object resolvedExpression = muleBeanExpressionResolver.evaluate(expressionWithoutSuffix, null);
        return resolvedExpression;
    }
}