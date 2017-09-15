/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.internal.processor.util.InvokerMessageProcessorUtil.splitArgumentsExpression;

import java.util.List;

import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class ArgumentSplitterTestCase extends AbstractMuleTestCase {

  @Test
  public void whenSingleMuleExpressionProcessedAsExpected() {

    List<String> args = splitArgumentsExpression("#[function(a)]");
    assertThat(args, hasSize(1));
    assertThat(args, hasItem("#[function(a)]"));
  }

  @Test
  public void whenSingleMuleExpressionWithInnerArrayProcessedAsExpected() {
    List<String> args = splitArgumentsExpression("#[function(a), [1, 2, 3]]");
    assertThat(args, hasSize(1));
    assertThat(args, hasItem("#[function(a),[1,2,3]]"));
  }

  @Test
  public void whenSingleMuleExpressionWithSeveralNestedArraysProcessedAsExpected() {
    List<String> args = splitArgumentsExpression("#[[[function(a), a, b, b], a], [1, 2, 3]]");
    assertThat(args, hasSize(1));
    assertThat(args, hasItem("#[[[function(a),a,b,b],a],[1,2,3]]"));
  }

  @Test
  public void whenMoreThanOneMuleExpressionsProcessedAsExpected() {
    List<String> args = splitArgumentsExpression("#[[[function(a), a, b, c], a]], #[1, 2, 3]");
    assertThat(args, hasSize(2));
    assertThat(args, hasItem("#[[[function(a),a,b,c],a]]"));
    assertThat(args, hasItem("#[1,2,3]"));
  }

  @Test
  public void whenThreeMuleExpressionsProcessedAsExpected() {
    List<String> args = splitArgumentsExpression("#[1, 2, 3], #[[[function(a), a, b, c], a]], #[[1, 2], [3, 4]]");
    assertThat(args, hasSize(3));
    assertThat(args, hasItem("#[1,2,3]"));
    assertThat(args, hasItem("#[[[function(a),a,b,c],a]]"));
    assertThat(args, hasItem("#[[1,2],[3,4]]"));
  }

  @Test
  public void whenValidExpressionSpacesWithinThenProcessAsExpected() {
    List<String> args = splitArgumentsExpression("      #[1, 2, 3]         ,       #[2, 3, 4] ");
    assertThat(args, hasSize(2));
    assertThat(args, hasItem("#[1,2,3]"));
    assertThat(args, hasItem("#[2,3,4]"));
  }
}
