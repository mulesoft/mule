/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.filters;

import static org.mule.compatibility.transport.http.HttpConnector.HTTP_PARAMS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_AUTHORIZATION;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 */
public class HttpBasicAuthenticationFilter extends org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(HttpBasicAuthenticationFilter.class);

  public HttpBasicAuthenticationFilter(String realm) {
    this.setRealm(realm);
  }

}
