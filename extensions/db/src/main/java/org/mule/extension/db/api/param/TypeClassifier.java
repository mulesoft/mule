/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A parameter group for configuring the type of a parameter.
 *
 * You can either configure a standard type or a custom one.
 *
 * @since 4.0
 */
@ExclusiveOptionals
public class TypeClassifier {

  /**
   * Standard type name. Use this attribute to specify
   * a standard JDBC type
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private JdbcType type;

  /**
   * Custom type name. Use this attribute to specify
   * a custom defined type
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private String customType;

  /**
   * @return The configured type as a {@link DbType} or {@code null} if none supplied
   */
  public DbType getDbType() {
    if (type != null) {
      return type.getDbType();
    } else if (customType != null) {
      return new DynamicDbType(customType);
    }

    return null;
  }
}
