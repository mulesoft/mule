/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
