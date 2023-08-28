/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
