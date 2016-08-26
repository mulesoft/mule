/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Allows specifying the type of a given parameter
 *
 * @since 4.0
 */
public class ParameterType {

  public ParameterType() {}

  public ParameterType(String paramName, JdbcType type) {
    this.paramName = paramName;
    this.type = type;
  }

  /**
   * The name of the input parameter.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String paramName;

  /**
   * Parameter type name.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private JdbcType type;

  public String getParamName() {
    return paramName;
  }

  public JdbcType getType() {
    return type;
  }
}
