/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.util.ASTIterator;
import org.mule.mvel2.util.ASTLinkedList;

/**
 * Base class {@link EnricherDataTypePropagator}
 */
public abstract class AbstractEnricherDataTypePropagator implements EnricherDataTypePropagator {

  @Override
  public boolean propagate(MuleEvent event, TypedValue typedValue, CompiledExpression compiledExpression) {
    ASTIterator iterator = new ASTLinkedList(compiledExpression.getFirstNode());

    if (iterator.hasMoreNodes()) {
      ASTNode node = iterator.nextNode();

      return doPropagate(event, typedValue, node);
    }

    return false;
  }

  protected abstract boolean doPropagate(MuleEvent event, TypedValue typedValue, ASTNode node);
}
