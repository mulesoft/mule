/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.exceptions;

import java.io.IOException;

/**
 * Thrown to indicate that there was an attempt of reading or writing more bytes that the allowed limit.
 *
 * @since 4.0
 */
public class LengthExceededException extends IOException {

  public LengthExceededException(String message) {
    super(message);
  }
}
