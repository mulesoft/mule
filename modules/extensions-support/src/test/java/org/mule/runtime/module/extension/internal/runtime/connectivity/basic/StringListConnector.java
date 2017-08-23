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
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.api.meta.ExpressionSupport;

import java.util.List;

@Extension(name = "StringList")
@Operations(VoidOperations.class)
public class StringListConnector {

  @Parameter
  private List<String> requiredListDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private List<String> requiredListNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private List<String> requiredListExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private List<String> requiredListExpressionSupporteds;

  @Parameter
  @Optional
  private List<String> optionalListDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private List<String> optionalListNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private List<String> optionalListExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private List<String> optionalListExpressionSupporteds;

}
