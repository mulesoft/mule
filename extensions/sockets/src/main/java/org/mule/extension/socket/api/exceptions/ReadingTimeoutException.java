/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.exceptions;

import java.io.IOException;

/**
 * Thrown to indicate that a timeout has been reached while awaiting for new data to arrive and be read.
 *
 * @since 4.0
 */
public class ReadingTimeoutException extends IOException {

  public ReadingTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
