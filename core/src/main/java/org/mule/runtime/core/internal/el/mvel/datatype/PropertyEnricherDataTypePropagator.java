/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.ast.AssignmentNode;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Propagates data type for inlined flow and session vars used for enrichment target
 */
public class PropertyEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator {

  @Override
  protected boolean doPropagate(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue, ASTNode node) {
    if (node instanceof AssignmentNode) {
      String assignmentVar = ((AssignmentNode) node).getAssignmentVar();

      if (event.getVariables().containsKey(assignmentVar)) {
        builder.addVariable(assignmentVar, typedValue.getValue(), typedValue.getDataType());
        return true;
      } else if (event.getSession().getPropertyNamesAsSet().contains(assignmentVar)) {
        event.getSession().setProperty(assignmentVar, typedValue.getValue(), typedValue.getDataType());
        return true;
      }
    }
    return false;
  }
}
