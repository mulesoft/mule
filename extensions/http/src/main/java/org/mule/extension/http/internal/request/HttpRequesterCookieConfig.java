/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import java.net.CookieManager;

/**
 * Provides access to the HTTP requester configuration relative to cookies.
 *
 * @since 4.0
 */
public interface HttpRequesterCookieConfig {

  /**
   * @return {@code true} if cookies have to be sent with the produced request.
   */
  boolean isEnableCookies();

  /**
   * @return the object where the cookies to be sent with the request and where the cookies sent with the response are stored.
   */
  CookieManager getCookieManager();

}
