/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.basic;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.api.meta.ExpressionSupport;

public class TestConnectionProvider implements ConnectionProvider<Object> {

  @Parameter
  private String connectionProviderRequiredFieldDefault;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private String connectionProviderRequiredFieldExpressionSupported;


  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private String connectionProviderRequiredFieldExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private String connectionProviderRequiredFieldExpressionNotSupported;

  @Parameter
  @Optional
  private String connectionProviderOptionalFieldDefault;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private String connectionProviderOptionalFieldExpressionSupported;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private String connectionProviderOptionalFieldExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private String connectionProviderOptionalFieldExpressionNotSupported;

  @Override
  public Object connect() throws ConnectionException {
    return new Object();
  }

  @Override
  public void disconnect(Object o) {

  }

  @Override
  public ConnectionValidationResult validate(Object o) {
    return ConnectionValidationResult.success();
  }
}
