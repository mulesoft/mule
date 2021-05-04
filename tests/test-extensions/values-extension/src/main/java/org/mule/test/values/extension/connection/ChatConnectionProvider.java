/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("chat")
public class ChatConnectionProvider implements ConnectionProvider<ChatConnection> {

  @Override
  public ChatConnection connect() throws ConnectionException {
    return new ChatConnection();
  }

  @Override
  public void disconnect(ChatConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(ChatConnection connection) {
    return success();
  }
}
