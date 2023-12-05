/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.internal.util.extension.connectivity.basic;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.api.meta.ExpressionSupport;

import java.util.Objects;

public class Owner {

  @Parameter
  private String requiredFieldDefault;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private String requiredFieldExpressionSupported;


  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private String requiredFieldExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private String requiredFieldExpressionNotSupported;

  @Parameter
  @Optional
  private String optionalFieldDefault;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private String optionalFieldExpressionSupported;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private String optionalFieldExpressionRequired;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private String optionalFieldExpressionNotSupported;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Owner owner = (Owner) o;
    return Objects.equals(requiredFieldDefault, owner.requiredFieldDefault) &&
        Objects.equals(requiredFieldExpressionSupported, owner.requiredFieldExpressionSupported) &&
        Objects.equals(requiredFieldExpressionRequired, owner.requiredFieldExpressionRequired) &&
        Objects.equals(requiredFieldExpressionNotSupported, owner.requiredFieldExpressionNotSupported) &&
        Objects.equals(optionalFieldDefault, owner.optionalFieldDefault) &&
        Objects.equals(optionalFieldExpressionSupported, owner.optionalFieldExpressionSupported) &&
        Objects.equals(optionalFieldExpressionRequired, owner.optionalFieldExpressionRequired) &&
        Objects.equals(optionalFieldExpressionNotSupported, owner.optionalFieldExpressionNotSupported);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredFieldDefault, requiredFieldExpressionSupported, requiredFieldExpressionRequired,
                        requiredFieldExpressionNotSupported, optionalFieldDefault, optionalFieldExpressionSupported,
                        optionalFieldExpressionRequired, optionalFieldExpressionNotSupported);
  }
}
