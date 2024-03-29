/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.some.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.exception.ModuleException;

public class CustomConnectionException extends ConnectionException {

  public CustomConnectionException(ModuleException e) {
    super("This is the message", e);
  }
}
