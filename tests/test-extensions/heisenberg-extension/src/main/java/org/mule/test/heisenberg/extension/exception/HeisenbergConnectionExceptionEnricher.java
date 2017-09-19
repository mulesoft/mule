/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.exception;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;

public class HeisenbergConnectionExceptionEnricher extends ExceptionHandler {

  public static final String ENRICHED_MESSAGE = "Enriched Connection Exception: ";

  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof MuleFatalException) {
      return e;
    }
    return new ConnectionException(ENRICHED_MESSAGE + e.getMessage(), e);
  }
}
