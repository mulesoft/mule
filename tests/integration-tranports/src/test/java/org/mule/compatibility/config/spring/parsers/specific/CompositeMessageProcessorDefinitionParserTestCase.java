/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.NullMessageProcessor;
import org.mule.tck.MuleTestUtils;

import org.junit.Test;

public class CompositeMessageProcessorDefinitionParserTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/specific/composite-message-processor.xml";
  }

  @Test
  public void testInterceptingCompositeOnEndpoint() throws Exception {
    EndpointBuilder endpointBuilder = lookupEndpointBuilder(muleContext.getRegistry(), "endpoint");
    InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
    assertEquals(2, endpoint.getMessageProcessors().size());

    Processor endpointProcessor =
        endpoint.getMessageProcessorsFactory().createInboundMessageProcessorChain(endpoint, getTestFlow(muleContext),
                                                                                  new NullMessageProcessor());
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    assertEquals("01231abc2", endpointProcessor.process(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of("0"))
        .build()).getMessageAsString(muleContext));
  }

}
