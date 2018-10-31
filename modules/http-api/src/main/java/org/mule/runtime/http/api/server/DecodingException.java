/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

/**
 * {@code DecodingException} Is an exception thrown when there is attempt to decode a malformed
 * or invalid text, url or url parameter.
 *
 * @since 1.2
 */
public class DecodingException extends HttpRequestParsingException {

  public DecodingException(String message, Throwable cause) {
    super(message, cause);
  }

}
