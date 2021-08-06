/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
