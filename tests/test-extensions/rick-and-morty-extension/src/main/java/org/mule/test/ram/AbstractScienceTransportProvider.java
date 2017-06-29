/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.ram;

import static java.util.Collections.singletonMap;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import java.io.ByteArrayInputStream;
import java.net.URL;

public abstract class AbstractScienceTransportProvider implements MessageDispatcherProvider<MessageDispatcher> {

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return request -> {
      try {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("test-http-response.xml");
        String response = String.format(IOUtils.toString(resource.openStream()), getResponseWord());
        return new DispatchingResponse(new ByteArrayInputStream(response.getBytes()), singletonMap("Content-Type", "text/xml"));
      } catch (Exception e) {
        throw new RuntimeException("Something went wrong when getting fake test response", e);
      }
    };
  }

  protected abstract String getResponseWord();

  @Override
  public void disconnect(MessageDispatcher connection) {}

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return ConnectionValidationResult.success();
  }
}
