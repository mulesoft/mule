/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.ram;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;

import java.io.ByteArrayInputStream;

public class TestHttpMessageDispatcherProvider implements MessageDispatcherProvider<MessageDispatcher> {

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return request -> new DispatchingResponse(new ByteArrayInputStream(new byte[0]), emptyMap());
  }

  @Override
  public void disconnect(MessageDispatcher connection) {

  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }
}
