/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.iterator;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A {@link ComponentAstSpliteratorStrategy} that navigates the whole AST using a {@link AstTraversalDirection} direction.
 *
 * @since 4.4.0
 */
public abstract class ComponentAstSpliteratorStrategy implements Spliterator<ComponentAst> {

  private final ComponentAst componentAst;
  private final AstTraversalDirection direction;

  private boolean rootProcessed = false;
  private Spliterator<ComponentAst> childrenSpliterator;
  private Spliterator<ComponentAst> currentChildSpliterator;

  public ComponentAstSpliteratorStrategy(ComponentAst componentAst, AstTraversalDirection direction) {
    this.componentAst = componentAst;
    this.direction = direction;
  }

  protected abstract boolean tryAdvanceBeforeChildrenConsumed(Consumer<? super ComponentAst> action);

  protected abstract boolean tryAdvanceAfterChildrenConsumed(Consumer<? super ComponentAst> action);

  protected boolean tryAdvanceOverComponentAst(Consumer<? super ComponentAst> action) {
    if (!rootProcessed) {
      rootProcessed = true;
      action.accept(componentAst);
      return true;
    }
    return false;
  }

  @Override
  public boolean tryAdvance(Consumer<? super ComponentAst> action) {
    if (tryAdvanceBeforeChildrenConsumed(action)) {
      return true;
    }

    trySplit();

    if (currentChildSpliterator != null) {
      if (currentChildSpliterator.tryAdvance(action)) {
        return true;
      } else {
        currentChildSpliterator = null;
        return tryAdvance(action);
      }
    } else {
      if (childrenSpliterator
          .tryAdvance(componentAst -> currentChildSpliterator = componentAst.recursiveSpliterator(direction))) {
        return tryAdvance(action);
      } else {
        return tryAdvanceAfterChildrenConsumed(action);
      }
    }
  }

  @Override
  public Spliterator<ComponentAst> trySplit() {
    if (childrenSpliterator == null) {
      childrenSpliterator = componentAst.directChildrenStream().spliterator();
    }
    return null;
  }

  @Override
  public long estimateSize() {
    return 1 + componentAst.directChildrenStream()
        .mapToLong(inner -> inner.recursiveSpliterator(direction).estimateSize())
        .sum();
  }

  @Override
  public int characteristics() {
    return ORDERED | DISTINCT | SIZED | NONNULL | IMMUTABLE | SUBSIZED;
  }

}
