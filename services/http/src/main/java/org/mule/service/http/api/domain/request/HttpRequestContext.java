/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.request;

/**
 *
 */
public interface HttpRequestContext {

  /**
   * @return the http request content
   */
  HttpRequest getRequest();

  /**
   * @return client connection descriptor
   */
  ClientConnection getClientConnection();

  /**
   * @return The scheme of the HTTP request URL (http or https)
   */
  String getScheme();

}
