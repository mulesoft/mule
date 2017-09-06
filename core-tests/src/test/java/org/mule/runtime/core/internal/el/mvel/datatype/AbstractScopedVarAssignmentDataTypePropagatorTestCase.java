/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.junit.Test;

public abstract class AbstractScopedVarAssignmentDataTypePropagatorTestCase
    extends AbstractVarAssignmentDataTypePropagatorTestCase {

  protected final String variableName;

  public AbstractScopedVarAssignmentDataTypePropagatorTestCase(EnricherDataTypePropagator dataTypePropagator,
                                                               String variableName) {
    super(dataTypePropagator);
    this.variableName = variableName;
  }

  @Test
  public void propagatesVarDataTypeUsingMapSyntax() throws Exception {
    doAssignmentDataTypePropagationTest(createAssignmentExpression("['" + PROPERTY_NAME + "']"));
  }

  @Test
  public void propagatesVarDataTypeUsingDotSyntax() throws Exception {
    doAssignmentDataTypePropagationTest(createAssignmentExpression("." + PROPERTY_NAME + ""));
  }

  @Test
  public void propagatesVarDataTypeUsingEscapedDotSyntax() throws Exception {
    doAssignmentDataTypePropagationTest(createAssignmentExpression(".'" + PROPERTY_NAME + "'"));
  }

  @Test
  public void doesNotChangesVarDataTypeUsingRecursiveMapSyntax() throws Exception {
    doInnerAssignmentDataTypePropagationTest(createAssignmentExpression("['" + PROPERTY_NAME + "']['" + INNER_PROPERTY_NAME
        + "']"));
  }

  @Test
  public void doesNotChangesVarDataTypeUsingRecursiveDotSyntax() throws Exception {
    doInnerAssignmentDataTypePropagationTest(createAssignmentExpression("." + PROPERTY_NAME + "." + INNER_PROPERTY_NAME));
  }

  @Test
  public void doesNotChangesVarDataTypeUsingRecursiveEscapedDotSyntax() throws Exception {
    doInnerAssignmentDataTypePropagationTest(createAssignmentExpression(".'" + PROPERTY_NAME + "'.'" + INNER_PROPERTY_NAME
        + "'"));
  }

  private String createAssignmentExpression(String accessorExpression) {
    return variableName + accessorExpression + " = 'unused'";
  }
}
