/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.TOP_DOWN;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.config.internal.model.iterator.BottomUpComponentAstSpliteratorTraversal;
import org.mule.runtime.config.internal.model.iterator.TopDownComponentAstSpliteratorTraversal;

import java.util.Spliterator;

/**
 * Factory that knows how to create a {@link Spliterator<ComponentAst>} to navigates the whole AST.
 *
 * @since 4.4.0
 */
public class ComponentAstSpliteratorStrategyFactory {

  private ComponentAstSpliteratorStrategyFactory() {}

  /**
   * Create a {@link Spliterator} that navigate the whole AST top down.
   *
   * @param componentAst the {@link ComponentAst} to navigate
   * @return a {@link Spliterator} that navigates the whole {@code componentAst} using {@link TOP_DOWN} direction.
   */
  public static Spliterator<ComponentAst> topDownTraversalStrategy(ComponentAst componentAst) {
    return traversalStrategy(componentAst, TOP_DOWN);
  }

  /**
   *  Create a {@link Spliterator} that navigate the whole AST bottom up.
   *
   * @param componentAst the {@link ComponentAst} to navigate
   * @return a {@link Spliterator} that navigates the whole {@code componentAst} using {@link BOTTOM_UP} direction.
   */
  public static Spliterator<ComponentAst> bottomUpTraversalStrategy(ComponentAst componentAst) {
    return traversalStrategy(componentAst, BOTTOM_UP);
  }

  /**
   *  Create a {@link Spliterator} that navigate the whole AST using {@link AstTraversalDirection} direction.
   *
   * @param componentAst the {@link ComponentAst} to navigate
   * @param direction the {@link AstTraversalDirection} used to navigate the whole AST.
   * @return a {@link Spliterator} that navigates the whole {@code componentAst} using the given {@code direction}.
   */
  public static Spliterator<ComponentAst> traversalStrategy(ComponentAst componentAst, AstTraversalDirection direction) {
    switch (direction) {
      case TOP_DOWN:
        return new TopDownComponentAstSpliteratorTraversal(componentAst);
      case BOTTOM_UP:
        return new BottomUpComponentAstSpliteratorTraversal(componentAst);
      default:
        throw new IllegalArgumentException("Unknown direction: " + direction);
    }
  }
}
