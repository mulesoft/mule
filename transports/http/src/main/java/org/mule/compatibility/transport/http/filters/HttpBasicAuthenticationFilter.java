/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 */
public class HttpBasicAuthenticationFilter extends org.mule.extension.http.api.listener.HttpBasicAuthenticationFilter {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(HttpBasicAuthenticationFilter.class);

  public HttpBasicAuthenticationFilter(String realm) {
    this.setRealm(realm);
  }

}
