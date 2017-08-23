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
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.api.meta.ExpressionSupport;

@Extension(name = "Basic")
@Operations(VoidOperations.class)
@ConnectionProviders(TestConnectionProvider.class)
public class TestConnector {

  @Parameter
  private Owner requiredPojoDefault;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private Owner requiredPojoNoExpression;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private Owner requiredPojoExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private Owner requiredPojoExpressionSupported;

  @Parameter
  @Optional
  private Owner optionalPojoDefault;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private Owner optionalPojoNoExpression;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private Owner optionalPojoExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private Owner optionalPojoExpressionSupported;

}
