/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.iterator;

import static org.mule.runtime.ast.api.util.AstTraversalDirection.TOP_DOWN;

import org.mule.runtime.ast.api.ComponentAst;

import java.util.function.Consumer;

/**
 * A {@link ComponentAstSpliteratorStrategy} that navigates the whole AST using {@link TOP_DOWN} direction.
 *
 * @since 4.4.0
 */
public class TopDownComponentAstSpliteratorTraversal extends ComponentAstSpliteratorStrategy {

  public TopDownComponentAstSpliteratorTraversal(ComponentAst componentAst) {
    super(componentAst, TOP_DOWN);
  }

  @Override
  protected boolean tryAdvanceBeforeChildrenConsumed(Consumer<? super ComponentAst> action) {
    return tryAdvanceOverComponentAst(action);
  }

  @Override
  protected boolean tryAdvanceAfterChildrenConsumed(Consumer<? super ComponentAst> action) {
    return false;
  }
}
