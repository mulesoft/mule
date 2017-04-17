/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

/**
 * Configuration of a custom parameter to extract from the token response.
 */
public class ParameterExtractor {

  /**
   * Identifier under which the extracted value will be stored in the OAuth authentication state.
   */
  @Parameter
  private String paramName;

  /**
   * MEL expression to extract the parameter value. This value can be later used by using the oauthContext function.
   */
  @Parameter
  private Literal<String> value;

  /**
   * @return name of the parameter used to store it in the oauth context.
   */
  public String getParamName() {
    return paramName;
  }

  /**
   * @return value extracted from the token response.
   */
  public Literal<String> getValue() {
    return value;
  }
}
