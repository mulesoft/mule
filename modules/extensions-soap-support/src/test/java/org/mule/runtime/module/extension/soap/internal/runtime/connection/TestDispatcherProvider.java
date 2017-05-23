/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class TestDispatcherProvider implements MessageDispatcherProvider<MessageDispatcher> {

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return new TestMessageDispatcher();
  }

  @Override
  public void disconnect(MessageDispatcher connection) {

  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }

  public class TestMessageDispatcher implements MessageDispatcher {

    private boolean disconnected = true;

    @Override
    public DispatchingResponse dispatch(DispatchingRequest context) {
      return new DispatchingResponse(new ByteArrayInputStream("".getBytes()), "text/xml", new HashMap<>());
    }

    @Override
    public void dispose() {
      disconnected = true;
    }

    @Override
    public void initialise() throws InitialisationException {
      disconnected = false;
    }

    public boolean isDisconnected() {
      return disconnected;
    }
  }
}
