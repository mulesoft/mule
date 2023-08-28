/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.exception.ModuleException;

public class CustomConnectionException extends ConnectionException {

  public CustomConnectionException(ModuleException e) {
    super("This is the message", e);
  }
}
