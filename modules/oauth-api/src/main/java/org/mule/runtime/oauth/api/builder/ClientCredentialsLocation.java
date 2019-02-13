/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

/**
 * Enum to define where to send the client credentials in an OAuth2 Dance.
 *
 * @since 4.2.0
 */
public enum ClientCredentialsLocation {

  /**
   * Send credentials in an Basic Authentication header
   */
  BASIC_AUTH_HEADER,

  /**
   * Send credentials encoded in HTTP Body
   */
  BODY,

  /**
   * Send credentials as query parameters
   */
  QUERY_PARAMS

}
