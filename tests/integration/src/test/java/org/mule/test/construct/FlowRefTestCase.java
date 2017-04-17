/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class FlowRefTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-ref.xml";
  }

  @Test
  public void twoFlowRefsToSubFlow() throws Exception {
    final Event muleEvent = flowRunner("flow1").withPayload("0").run();
    assertThat(getPayloadAsString(muleEvent.getMessage()), is("012xyzabc312xyzabc3"));
  }

  @Test
  public void dynamicFlowRef() throws Exception {
    assertEquals("0A",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "A").run().getMessageAsString(muleContext));
    assertEquals("0B",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "B").run().getMessageAsString(muleContext));
  }

  @Test
  public void dynamicFlowRefWithChoice() throws Exception {
    assertEquals("0A",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "C").run().getMessageAsString(muleContext));
  }

  @Test
  public void dynamicFlowRefWithScatterGather() throws Exception {
    List<InternalMessage> messageList =
        (List<InternalMessage>) flowRunner("flow2").withPayload("0").withVariable("letter", "SG").run().getMessage()
            .getPayload().getValue();

    List payloads = messageList.stream().map(msg -> msg.getPayload().getValue()).collect(toList());
    assertEquals("0A", payloads.get(0));
    assertEquals("0B", payloads.get(1));
  }

  @Test(expected = MessagingException.class)
  public void flowRefNotFound() throws Exception {
    assertEquals("0C",
                 flowRunner("flow2").withPayload("0").withVariable("letter", "Z").run().getMessageAsString(muleContext));
  }

}
