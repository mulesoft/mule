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

import java.util.Map;

@Extension(name = "Map")
@Operations(VoidOperations.class)
public class MapConnector {

  @Parameter
  private Map<String, Object> requiredMapDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private Map<String, Object> requiredMapNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private Map<String, Object> requiredMapExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  private Map<String, Object> requiredMapExpressionSupporteds;

  @Parameter
  @Optional
  private Map<String, Object> optionalMapDefaults;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional
  private Map<String, Object> optionalMapNoExpressions;

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  @Optional
  private Map<String, Object> optionalMapExpressionRequireds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  private Map<String, Object> optionalMapExpressionSupporteds;

}
