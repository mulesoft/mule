/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.runtime.core.message.BaseAttributes;
import org.mule.runtime.core.model.ParameterMap;

/**
 * Base representation of HTTP message attributes.
 *
 * @since 4.0
 */
public abstract class HttpAttributes extends BaseAttributes {

  /**
   * Map of HTTP headers in the message. Former properties.
   */
  protected final ParameterMap headers;

  public HttpAttributes(ParameterMap headers) {
    this.headers = headers.toImmutableParameterMap();
  }

  public ParameterMap getHeaders() {
    return headers;
  }
}
