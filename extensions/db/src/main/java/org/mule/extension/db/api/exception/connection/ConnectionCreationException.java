/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.api.exception.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Thrown to indicate an error creating a connection
 */
public class ConnectionCreationException extends ConnectionException {

  public ConnectionCreationException(String message) {
    super(message);
  }

  public ConnectionCreationException(Throwable throwable) {
    super(throwable);
  }

  public ConnectionCreationException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public ConnectionCreationException(String message, Throwable throwable, DbError dbError) {
    super(message, new ModuleException(throwable, dbError));
  }
}
