/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.basic;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.api.meta.ExpressionSupport;

@Extensible
public class ExtensibleOwner {

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

}
