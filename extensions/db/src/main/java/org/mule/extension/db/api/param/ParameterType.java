/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;

/**
 * Allows specifying the type of a given parameter
 *
 * @since 4.0
 */
public class ParameterType {

  public ParameterType() {}

  public ParameterType(String key, TypeClassifier typeClassifier) {
    this.key = key;
    this.typeClassifier = typeClassifier;
  }

  /**
   * The name of the input parameter.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String key;

  @ParameterGroup
  private TypeClassifier typeClassifier;

  public String getKey() {
    return key;
  }

  public DbType getDbType() {
    return typeClassifier.getDbType();
  }
}
