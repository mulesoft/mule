/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.MESSAGE_PAYLOAD;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.PAYLOAD;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.ast.Assignment;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.metadata.DefaultTypedValue;

/**
 * Propagates data type when payload is used as enrichment target
 */
public class PayloadEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator {

  @Override
  protected boolean doPropagate(Event event, Event.Builder builder, DefaultTypedValue typedValue, ASTNode node) {
    if (node instanceof Assignment) {
      String assignmentVar = ((Assignment) node).getAssignmentVar();

      if (PAYLOAD.equals(assignmentVar) || MESSAGE_PAYLOAD.equals(assignmentVar)) {
        builder.message(InternalMessage.builder(event.getMessage()).payload(typedValue.getValue())
            .mediaType(typedValue.getDataType().getMediaType()).build());
        return true;
      }

    }
    return false;
  }

}
