/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.Accessor;
import org.mule.mvel2.compiler.AccessorNode;
import org.mule.mvel2.compiler.ExecutableLiteral;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessor;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessorNest;
import org.mule.mvel2.optimizers.impl.refl.nodes.VariableAccessor;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Base class for extracting data type from map variables
 */
public abstract class AbstractVariableExpressionDataTypeResolver extends AbstractExpressionDataTypeResolver {

  private final String propertyName;

  public AbstractVariableExpressionDataTypeResolver(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  protected DataType getDataType(PrivilegedEvent event, ASTNode node) {
    final Accessor accessor = node.getAccessor();

    if (accessor instanceof VariableAccessor) {
      VariableAccessor variableAccessor = (VariableAccessor) accessor;
      if (variableAccessor.getProperty().equals(propertyName)) {
        final AccessorNode nextNode = variableAccessor.getNextNode();
        String propertyName = null;
        if (nextNode instanceof MapAccessorNest) {
          final MapAccessorNest mapAccesorNest = (MapAccessorNest) nextNode;
          if (mapAccesorNest.getProperty().isLiteralOnly()) {
            propertyName = (String) ((ExecutableLiteral) mapAccesorNest.getProperty()).getLiteral();
          }
        } else if (nextNode instanceof MapAccessor) {
          propertyName = (String) ((MapAccessor) nextNode).getProperty();
        }

        if (propertyName != null) {
          return getVariableDataType(event, propertyName);
        }
      }
    }

    return null;
  }

  protected abstract DataType getVariableDataType(PrivilegedEvent event, String propertyName);

}
