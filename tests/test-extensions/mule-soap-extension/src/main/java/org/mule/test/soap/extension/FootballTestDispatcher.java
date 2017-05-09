/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.emptyMap;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;

import java.net.URL;

public class FootballTestDispatcher implements MessageDispatcher {

  @Override
  public void dispose() {

  }

  @Override
  public DispatchingResponse dispatch(DispatchingRequest context) {
    try {
      URL resource = Thread.currentThread().getContextClassLoader().getResource("test-http-response.xml");
      return new DispatchingResponse(resource.openStream(), "text/xml", emptyMap());
    } catch (Exception e) {
      throw new RuntimeException("Something went wrong when getting fake test response", e);
    }
  }

  @Override
  public void initialise() throws InitialisationException {

  }
}
