/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  @Override
  protected boolean mockExprExecutorService() {
    return true;
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
