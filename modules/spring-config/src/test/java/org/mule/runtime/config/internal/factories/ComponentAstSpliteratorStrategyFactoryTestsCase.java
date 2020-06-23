/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.TOP_DOWN;
import static org.mule.runtime.config.internal.factories.ComponentAstSpliteratorStrategyFactory.bottomUpTraversalStrategy;
import static org.mule.runtime.config.internal.factories.ComponentAstSpliteratorStrategyFactory.topDownTraversalStrategy;
import static org.mule.runtime.config.internal.factories.ComponentAstSpliteratorStrategyFactory.traversalStrategy;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.iterator.BottomUpComponentAstSpliteratorTraversal;
import org.mule.runtime.config.internal.model.iterator.TopDownComponentAstSpliteratorTraversal;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Spliterator;

public class ComponentAstSpliteratorStrategyFactoryTestsCase extends AbstractMuleTestCase {

  private ComponentAst componentAst;

  @Before
  public void setup() {
    componentAst = baseComponentBuilder("root").build();
  }

  @Test
  public void createTopDownTraversalStrategy() {
    Spliterator<ComponentAst> componentAstSpliterator = topDownTraversalStrategy(componentAst);
    assertThat(componentAstSpliterator, is(notNullValue()));
    assertThat(componentAstSpliterator, is(instanceOf(TopDownComponentAstSpliteratorTraversal.class)));
  }

  @Test
  public void createBottomUpTraversalStrategy() {
    Spliterator<ComponentAst> componentAstSpliterator = bottomUpTraversalStrategy(componentAst);
    assertThat(componentAstSpliterator, is(notNullValue()));
    assertThat(componentAstSpliterator, is(instanceOf(BottomUpComponentAstSpliteratorTraversal.class)));
  }

  @Test
  public void createTraversalStrategy() {
    Spliterator<ComponentAst> componentAstSpliterator = traversalStrategy(componentAst, TOP_DOWN);
    assertThat(componentAstSpliterator, is(notNullValue()));
    assertThat(componentAstSpliterator, is(instanceOf(TopDownComponentAstSpliteratorTraversal.class)));

    componentAstSpliterator = traversalStrategy(componentAst, BOTTOM_UP);
    assertThat(componentAstSpliterator, is(notNullValue()));
    assertThat(componentAstSpliterator, is(instanceOf(BottomUpComponentAstSpliteratorTraversal.class)));
  }

  private ComponentModel.Builder baseComponentBuilder(String name) {
    return new ComponentModel.Builder()
        .setIdentifier(ComponentIdentifier.builder().namespace("lalala").name(name).build());
  }
}
