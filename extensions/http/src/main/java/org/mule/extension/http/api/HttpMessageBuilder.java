/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.HashMap;
import java.util.Map;

/**
 * Base component to create HTTP messages.
 *
 * @since 4.0
 */
public class HttpMessageBuilder {

  /**
   * HTTP headers the message should include.
   */
  @Parameter
  @Optional
  @Content
  protected Map<String, String> headers = new HashMap<>();

  public Map<String, String> getHeaders() {
    return headers;
  }

}
