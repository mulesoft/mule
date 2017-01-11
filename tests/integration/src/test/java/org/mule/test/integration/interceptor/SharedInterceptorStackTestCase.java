/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interceptor;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class SharedInterceptorStackTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "shared-interceptor-stack-flow.xml";
  }

  @Test
  public void testSharedInterceptorOnServiceOne() throws Exception {
    InternalMessage response = flowRunner("serviceOne").withPayload(TEST_MESSAGE).run().getMessage();
    assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentOne", response.getPayload().getValue());
  }

  @Test
  public void testSharedInterceptorOnServiceTwo() throws Exception {
    InternalMessage response = flowRunner("serviceTwo").withPayload(TEST_MESSAGE).run().getMessage();
    assertEquals(TEST_MESSAGE + " CustomInterceptor ComponentTwo", response.getPayload().getValue());
  }

  public static class CustomInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor {

    @Override
    public Event process(Event event) throws MuleException {
      return processNext(Event.builder(event).message(InternalMessage.builder(event.getMessage())
          .payload(event.getMessage().getPayload().getValue().toString() + " CustomInterceptor").build()).build());
    }

    @Override
    public Processor getNext() {
      return next;
    }
  }

  public static class CustomComponent {

    private String appendString;

    public String process(String input) {
      return input + appendString;
    }

    public void setAppendString(String string) {
      this.appendString = string;
    }
  }
}
