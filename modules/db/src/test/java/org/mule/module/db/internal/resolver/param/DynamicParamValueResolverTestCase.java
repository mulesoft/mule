/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class DynamicParamValueResolverTestCase extends AbstractQueryParamResolverTestCase
{

    private final ExpressionManager expressionManager = mock(ExpressionManager.class);
    private final DynamicParamValueResolver paramResolver = new DynamicParamValueResolver(expressionManager);

    @Test
    public void resolvesStaticParam() throws Exception
    {
        final String expectedValue = "test";
        List<QueryParamValue> templateParams = getQueryParamValues(expectedValue);

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams.size(), equalTo(1));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo(expectedValue));
        verify(expressionManager, times(0)).evaluate(anyString(), eq(muleEvent));
    }

    @Test
    public void resolvesExpressionParam() throws Exception
    {
        final String expectedParamValue = "foo";
        String paramExpressionValue = "#[payload]";
        addResolvableExpression(expectedParamValue, paramExpressionValue);

        List<QueryParamValue> templateParams = getQueryParamValues(paramExpressionValue);

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams.size(), equalTo(1));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo(expectedParamValue));
    }

    @Test
    public void resolvesMultipleParams() throws Exception
    {
        final String expectedParamValue1 = "foo";
        String expectedParamValue2 = "bar";

        String paramExpressionValue = "#[payload]";
        addResolvableExpression(expectedParamValue1, paramExpressionValue);
        List<QueryParamValue> templateParams = getQueryParamValues(paramExpressionValue, expectedParamValue2);

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams.size(), equalTo(2));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo(expectedParamValue1));
        assertThat((String) resolvedParams.get(1).getValue(), equalTo(expectedParamValue2));
    }

    protected void addResolvableExpression(String expectedParamValue, String paramExpressionValue)
    {
        when(expressionManager.isExpression(paramExpressionValue)).thenReturn(true);
        when(expressionManager.evaluate(paramExpressionValue, muleEvent)).thenReturn(expectedParamValue);
    }

}
