/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.basic;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.api.meta.ExpressionSupport;

@Extension(name = "GlobalPojo")
@Operations(VoidOperations.class)
public class GlobalPojoConnector {

  /**
   * This should generate a Global element for the Owner, but no child element inside the config
   */
  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private Owner requiredPojoExpressionRequired;

  @Parameter
  private ExtensibleOwner requiredExtensiblePojo;

}
