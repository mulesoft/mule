/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.util.ASTIterator;
import org.mule.mvel2.util.ASTLinkedList;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Base class for extracting data type from {@link CompiledExpression}
 */
public abstract class AbstractExpressionDataTypeResolver implements ExpressionDataTypeResolver {

  @Override
  public DataType resolve(PrivilegedEvent event, CompiledExpression compiledExpression) {
    ASTIterator iterator = new ASTLinkedList(compiledExpression.getFirstNode());

    if (!iterator.hasMoreNodes()) {
      return null;
    } else {
      ASTNode node = iterator.nextNode();

      return getDataType(event, node);
    }
  }

  protected abstract DataType getDataType(PrivilegedEvent event, ASTNode node);
}
