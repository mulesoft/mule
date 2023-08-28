/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.util.ASTIterator;
import org.mule.mvel2.util.ASTLinkedList;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Base class {@link EnricherDataTypePropagator}
 */
public abstract class AbstractEnricherDataTypePropagator implements EnricherDataTypePropagator {

  @Override
  public boolean propagate(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue,
                           CompiledExpression compiledExpression) {
    ASTIterator iterator = new ASTLinkedList(compiledExpression.getFirstNode());

    if (iterator.hasMoreNodes()) {
      ASTNode node = iterator.nextNode();

      return doPropagate(event, builder, typedValue, node);
    }

    return false;
  }

  protected abstract boolean doPropagate(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue,
                                         ASTNode node);
}
