/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.module.cxf.builder.WebServiceMessageProcessorBuilder;
import org.mule.compatibility.module.cxf.testmodels.Echo;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

public class CxfInboundMessageProcessorTestCase extends AbstractMuleContextTestCase {

  String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
      + "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.module.compatibility.mule.org/\">" + "<text>echo</text>" + "</ns1:echo>"
      + "</soap:Body></soap:Envelope>";

  boolean gotEvent = false;
  Object payload;

  @Test
  public void testInbound() throws Exception {
    CxfInboundMessageProcessor processor = createCxfMessageProcessor();

    Processor messageProcessor = event -> {
      payload = event.getMessage().getPayload().getValue();
      assertEquals("echo", payload);
      gotEvent = true;
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("echo").build()).build();
    };
    processor.setListener(messageProcessor);

    Event response = processor.process(eventBuilder().message(InternalMessage.of(msg)).build());

    Object payload = response.getMessage().getPayload().getValue();
    assertTrue(payload instanceof OutputHandler);

    ((OutputHandler) payload).write(response, new NullOutputStream());
    assertTrue(gotEvent);
  }

  @Test
  public void testOneWay() throws Exception {
    CxfInboundMessageProcessor processor = createCxfMessageProcessor();

    Processor messageProcessor = event -> {
      payload = event.getMessage().getPayload().getValue();
      assertEquals("echo", payload);
      gotEvent = true;
      return null;
    };
    processor.setListener(messageProcessor);

    Event response = processor.process(eventBuilder().message(InternalMessage.of(msg)).build());

    assertTrue(gotEvent);
    assertNull(response);
  }

  private CxfInboundMessageProcessor createCxfMessageProcessor() throws Exception {
    CxfConfiguration config = new CxfConfiguration();
    config.setMuleContext(muleContext);
    config.initialise();

    // Build a CXF MessageProcessor
    WebServiceMessageProcessorBuilder builder = new WebServiceMessageProcessorBuilder();
    builder.setConfiguration(config);
    builder.setServiceClass(Echo.class);
    builder.setMuleContext(muleContext);
    builder.setFlowConstruct(getTestFlow(muleContext));

    CxfInboundMessageProcessor processor = builder.build();
    processor.initialise();
    processor.start();
    return processor;
  }

}
