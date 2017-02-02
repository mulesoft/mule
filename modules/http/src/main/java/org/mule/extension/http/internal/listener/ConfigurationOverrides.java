/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class ConfigurationOverrides {

  /**
   * By default, the request will be parsed (for example, a multi part request will be mapped as a Mule message with null payload
   * and inbound attachments with each part). If this property is set to false, no parsing will be done, and the payload will
   * always contain the raw contents of the HTTP request.
   */
  @Parameter
  @Optional
  private Boolean parseRequest;

  /**
   * Ideal for proxy scenarios, this indicates whether errors produced by an HTTP request should be interpreted by the listener.
   * If enabled, an error thrown by an HTTP request operation reaching a listener will be analysed for response data, so if a
   * request operation throws a FORBIDDEN error, for example, then the listener will generate a 403 error response.
   */
  @Parameter
  @Optional
  private Boolean interpretRequestErrors;

  public Boolean getParseRequest() {
    return parseRequest;
  }

  public Boolean getInterpretRequestErrors() {
    return interpretRequestErrors;
  }
}
