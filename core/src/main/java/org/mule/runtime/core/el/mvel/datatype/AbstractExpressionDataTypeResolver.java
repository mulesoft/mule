/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.util.ASTIterator;
import org.mule.mvel2.util.ASTLinkedList;

/**
 * Base class for extracting data type from {@link CompiledExpression}
 */
public abstract class AbstractExpressionDataTypeResolver implements ExpressionDataTypeResolver {

  @Override
  public DataType resolve(MuleEvent event, CompiledExpression compiledExpression) {
    ASTIterator iterator = new ASTLinkedList(compiledExpression.getFirstNode());

    if (!iterator.hasMoreNodes()) {
      return null;
    } else {
      ASTNode node = iterator.nextNode();

      return getDataType(event, node);
    }
  }

  protected abstract DataType getDataType(MuleEvent event, ASTNode node);
}
