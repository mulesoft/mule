/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.MESSAGE_PAYLOAD;
import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.PAYLOAD;

import org.mule.mvel2.ast.ASTNode;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Resolves data type for expressions representing message's payload
 */
public class PayloadExpressionDataTypeResolver extends AbstractExpressionDataTypeResolver {

  @Override
  protected DataType getDataType(PrivilegedEvent event, ASTNode node) {
    if (node.isIdentifier() && (PAYLOAD.equals(node.getName()) || MESSAGE_PAYLOAD.equals(node.getName()))) {
      return event.getMessage().getPayload().getDataType();
    } else {
      return null;
    }
  }
}
