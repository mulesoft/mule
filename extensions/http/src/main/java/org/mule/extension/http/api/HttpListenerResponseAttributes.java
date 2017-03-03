/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.service.http.api.domain.ParameterMap;

/**
 * HTTP listener specific {@link HttpResponseAttributes}. Unless interpretation of request errors is enabled, only this kind of
 * attributes will be considered within an error message.
 *
 * @since 4.0
 */
public class HttpListenerResponseAttributes extends HttpResponseAttributes {

  public HttpListenerResponseAttributes(int statusCode, String reasonPhrase, ParameterMap headers) {
    super(statusCode, reasonPhrase, headers);
  }

}
