/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.request;

import org.mule.service.http.api.domain.HttpMessage;
import org.mule.service.http.api.domain.HttpProtocol;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;

public interface HttpRequest extends HttpMessage {

  /**
   * @return the protocol version
   */
  HttpProtocol getProtocol();

  /**
   * @return the request path extracted from the uri
   */
  String getPath();

  /**
   * @return the request http method
   */
  String getMethod();

  /**
   * @return the request uri
   */
  String getUri();

  ParameterMap getQueryParams();

  /**
   * @return the raw input stream from the body. if there's not body then returns null. After calling this method #getEntity
   *         should not be used.
   */
  InputStreamHttpEntity getInputStreamEntity();
}
