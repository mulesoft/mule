/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.module.cxf.CxfConfiguration;
import org.mule.compatibility.module.cxf.CxfOutboundMessageProcessor;
import org.mule.compatibility.module.cxf.builder.SimpleClientMessageProcessorBuilder;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.component.simple.EchoService;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class CxfOutboundMessageProcessorTestCase extends AbstractMuleContextTestCase {

  String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
      + "<ns1:echo xmlns:ns1=\"http://simple.component.api.core.runtime.mule.org/\">"
      + "<ns1:return>" + TEST_PAYLOAD + "</ns1:return>"
      + "</ns1:echo>" + "</soap:Body></soap:Envelope>";

  boolean gotEvent = false;
  Object payload;

  @Test
  public void testOutbound() throws Exception {
    CxfConfiguration config = new CxfConfiguration();
    config.setMuleContext(muleContext);
    config.initialise();

    // Build a CXF MessageProcessor
    SimpleClientMessageProcessorBuilder builder = new SimpleClientMessageProcessorBuilder();
    builder.setConfiguration(config);
    builder.setServiceClass(EchoService.class);
    builder.setOperation("echo");
    builder.setMuleContext(muleContext);

    CxfOutboundMessageProcessor processor = builder.build();

    Processor messageProcessor = event -> {
      payload = event.getMessage().getPayload().getValue();
      try {
        System.out.println(getPayloadAsString(event.getMessage()));
      } catch (Exception e) {
        e.printStackTrace();
      }
      gotEvent = true;
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(msg).build()).build();
    };
    processor.setListener(messageProcessor);

    Event response = processor.process(eventBuilder().message(InternalMessage.of(TEST_PAYLOAD)).build());
    assertThat(processor.getClient().getRequestContext().isEmpty(), is(true));
    assertThat(processor.getClient().getResponseContext().isEmpty(), is(true));
    Object payload = response.getMessage().getPayload().getValue();
    assertThat(payload, instanceOf(String.class));
    assertThat((String) payload, is(TEST_PAYLOAD));
    assertThat(gotEvent, is(true));
  }

}
