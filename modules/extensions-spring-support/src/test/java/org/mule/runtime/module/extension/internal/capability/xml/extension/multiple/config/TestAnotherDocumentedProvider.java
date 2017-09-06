/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 * Another Provider Documentation
 */
@Alias("another")
public class TestAnotherDocumentedProvider implements ConnectionProvider<String> {

  @Override
  public String connect() throws ConnectionException {
    return "Magic Connection";
  }

  @Override
  public void disconnect(String connection) {

  }

  @Override
  public ConnectionValidationResult validate(String connection) {
    return success();
  }
}
