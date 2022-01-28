/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.exception;

public class RequestHandlerAlreadyPresentException extends Exception {

  private static final long serialVersionUID = 1876037969785787575L;

  public RequestHandlerAlreadyPresentException(String path) {
    super("A request handler for the path '" + path + "' already exists");
  }
}
