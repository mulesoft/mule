/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.MESSAGE_PAYLOAD;
import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.PAYLOAD;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.ast.Assignment;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Propagates data type when payload is used as enrichment target
 */
public class PayloadEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator {

  @Override
  protected boolean doPropagate(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue, ASTNode node) {
    if (node instanceof Assignment) {
      String assignmentVar = ((Assignment) node).getAssignmentVar();

      if (PAYLOAD.equals(assignmentVar) || MESSAGE_PAYLOAD.equals(assignmentVar)) {
        builder.message(Message.builder(event.getMessage()).value(typedValue.getValue())
            .mediaType(typedValue.getDataType().getMediaType()).build());
        return true;
      }

    }
    return false;
  }

}
