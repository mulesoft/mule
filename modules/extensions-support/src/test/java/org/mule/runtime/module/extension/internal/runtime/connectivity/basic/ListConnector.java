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

@Extension(name = "List")
@Operations(VoidOperations.class)
public class ListConnector {

  @Parameter
  private List<Object> requiredListObjectsDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private List<Object> requiredListObjectsNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private List<Object> requiredListObjectsExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private List<Object> requiredListObjectsExpressionSupporteds;

  @Parameter
  private List<Account> requiredListDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private List<Account> requiredListNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private List<Account> requiredListExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private List<Account> requiredListExpressionSupporteds;

  @Parameter
  @Optional
  private List<Object> optionalListDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private List<Object> optionalListNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private List<Object> optionalListExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private List<Object> optionalListExpressionSupporteds;

}
