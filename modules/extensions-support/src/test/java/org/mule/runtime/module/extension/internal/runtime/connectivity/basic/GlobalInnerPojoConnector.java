/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.basic;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.api.meta.ExpressionSupport;

@Extension(name = "InnerPojo")
@Operations(VoidOperations.class)
public class GlobalInnerPojoConnector {

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private Account requiredPojoExpressionRequired;

  /**
   * This should generate a Global element for the inner Owner, but no child element inside the account POJO
   */
  @Parameter
  private Account requiredPojoDefault;
}
