/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.MuleTestUtils;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class CompositeMessageProcessorDefinitionParserTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/specific/composite-message-processor.xml";
  }

  @Test
  public void testInterceptingComposite() throws Exception {
    Processor composite = muleContext.getRegistry().lookupObject("composite1");
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    assertEquals("0123", composite.process(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of("0"))
        .build()).getMessageAsString(muleContext));
  }

  @Test
  public void testInterceptingNestedComposite() throws Exception {
    Processor composite = muleContext.getRegistry().lookupObject("composite2");
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    assertEquals("01abc2", composite.process(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of("0"))
        .build()).getMessageAsString(muleContext));
  }

}
