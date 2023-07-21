/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
