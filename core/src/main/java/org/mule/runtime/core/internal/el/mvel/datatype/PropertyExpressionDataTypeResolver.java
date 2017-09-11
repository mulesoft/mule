/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.ast.ASTNode;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Resolves data type for expressions representing a reference to a invocation or session variable.
 */
public class PropertyExpressionDataTypeResolver extends AbstractExpressionDataTypeResolver {

  @Override
  protected DataType getDataType(PrivilegedEvent event, ASTNode node) {
    if (node.isIdentifier() && event.getVariables().containsKey(node.getName())) {
      return event.getVariables().get(node.getName()).getDataType();
    } else if (node.isIdentifier() && event.getSession().getPropertyNamesAsSet().contains(node.getName())) {
      return event.getSession().getPropertyDataType(node.getName());
    } else {
      return null;
    }
  }
}
