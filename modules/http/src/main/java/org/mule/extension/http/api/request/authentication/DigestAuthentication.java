/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import static org.mule.service.http.api.client.HttpAuthenticationType.DIGEST;
import org.mule.service.http.api.client.HttpRequestAuthentication;

/**
 * Configures digest authentication for the requests.
 *
 * @since 4.0
 */
public class DigestAuthentication extends UsernamePasswordAuthentication {

  @Override
  public HttpRequestAuthentication buildRequestAuthentication() {
    return getBaseRequestAuthentication(DIGEST);
  }
}
