/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.processor.util.InvokerMessageProcessorUtil.splitAndMergeArgumentsExpression;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.Test;

public class ArgumentSplitterTestCase extends AbstractMuleTestCase
{
    @Test
    public void whenSingleMuleExpressionProcessedAsExpected()
    {

        List<String> args = splitAndMergeArgumentsExpression("#[function(a)]");
        assertThat(args.get(0), equalTo("#[function(a)]"));

    }

    @Test
    public void whenSingleMuleExpressionWithInnerArrayProcessedAsExpected()
    {
        List<String> args = splitAndMergeArgumentsExpression("#[function(a), [1, 2, 3]]");
        assertThat(args.get(0), equalTo("#[function(a), [1, 2, 3]]"));
    }

    @Test
    public void whenSingleMuleExpressionWithSeveralNestedArraysProcessedAsExpected()
    {
        List<String> args = splitAndMergeArgumentsExpression("#[[[function(a), a, b, b], a], [1, 2, 3]]");
        assertThat(args.get(0), equalTo("#[[[function(a), a, b, b], a], [1, 2, 3]]"));
    }

    @Test
    public void whenMoreThanOneMuleExpressionsProcessedAsExpected()
    {
        List<String> args = splitAndMergeArgumentsExpression("#[[[function(a), a, b, c], a]], #[1, 2, 3]");
        assertThat(args.get(0), equalTo("#[[[function(a), a, b, c], a]]"));
        assertThat(args.get(1), equalTo("#[1, 2, 3]"));
    }

    @Test
    public void whenThreeMuleExpressionsProcessedAsExpected()
    {
        List<String> args = splitAndMergeArgumentsExpression("#[1, 2, 3], #[[[function(a), a, b, c], a]], #[[1, 2], [3, 4]]");
        assertThat(args.get(0), equalTo("#[1, 2, 3]"));
        assertThat(args.get(1), equalTo("#[[[function(a), a, b, c], a]]"));
        assertThat(args.get(2), equalTo("#[[1, 2], [3, 4]]"));
    }

    @Test
    public void whenValidExpressionSpacesWithinThenProcessAsExpected()
    {
        List<String> args = splitAndMergeArgumentsExpression("      #[1, 2, 3]         ,       #[2, 3, 4] ");
        assertThat(args.get(0), equalTo("#[1, 2, 3]"));
        assertThat(args.get(1), equalTo("#[2, 3, 4]"));
    }
}
