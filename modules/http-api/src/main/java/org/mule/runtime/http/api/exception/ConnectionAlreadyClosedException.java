/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.exception;

import java.io.IOException;

/**
 * This Exception is thrown when there is an error that implies losing the connection with the client.
 * 
 * @since 4.4.0
 */
public class ConnectionAlreadyClosedException extends IOException {

  private static final long serialVersionUID = 1271427847005415136L;

  public ConnectionAlreadyClosedException(Throwable cause) {
    super(cause);
  }
}
