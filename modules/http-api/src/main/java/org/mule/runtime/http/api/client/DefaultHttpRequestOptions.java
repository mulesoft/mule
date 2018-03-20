/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.runtime.http.api.client.auth.HttpAuthentication;

/**
 * Basic implementation of {@link HttpRequestOptions}. Instances can only be obtained through {@link HttpRequestOptionsBuilder}.
 *
 * @since 4.2
 */
class DefaultHttpRequestOptions implements HttpRequestOptions {

  private int responseTimeout;
  private boolean followsRedirect;
  private HttpAuthentication authentication;

  DefaultHttpRequestOptions(int responseTimeout, boolean followsRedirect, HttpAuthentication authentication) {
    this.responseTimeout = responseTimeout;
    this.followsRedirect = followsRedirect;
    this.authentication = authentication;
  }

  @Override
  public int getResponseTimeout() {
    return responseTimeout;
  }

  @Override
  public boolean isFollowsRedirect() {
    return followsRedirect;
  }

  @Override
  public HttpAuthentication getAuthentication() {
    return authentication;
  }

}
