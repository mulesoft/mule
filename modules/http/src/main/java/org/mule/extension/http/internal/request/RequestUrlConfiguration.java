/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public final class RequestUrlConfiguration {

  /**
   * Base path to use for all requests that reference this config.
   */
  @Parameter
  @Optional(defaultValue = "/")
  private String basePath;

  public String getBasePath() {
    return basePath;
  }
}
