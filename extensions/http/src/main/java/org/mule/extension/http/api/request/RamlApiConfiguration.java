/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request;


import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Configuration for the API that is being consumed based on a RAML file.
 *
 * @since 4.0
 */
@Extensible
public class RamlApiConfiguration {

  /**
   * The location of the RAML file.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @DisplayName("RAML Location")
  private String location;

  public String getLocation() {
    return location;
  }
}
