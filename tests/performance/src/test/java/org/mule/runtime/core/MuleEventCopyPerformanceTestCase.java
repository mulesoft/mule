/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleEventCopyPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Mock
  private Flow flow;

  private String payload;
  private DefaultMuleEvent event;

  @Override
  public int getTestTimeoutSecs() {
    return 120;
  }

  @Before
  public void before() throws IOException {
    payload = IOUtils.getResourceAsString("test-data.json", getClass());
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copy() throws Exception {
    event = createMuleEvent();
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(event.getMessage(), event);
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10Properties() throws Exception {
    event = createMuleEventWithFlowVarsAndProperties(10);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(event.getMessage(), event);
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50Properties() throws Exception {
    event = createMuleEventWithFlowVarsAndProperties(50);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(event.getMessage(), event);
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PopertiesWrite1OfEach() throws Exception {
    event = createMuleEventWithFlowVarsAndProperties(10);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(event.getMessage(), event);
      event.setFlowVariable("newKey", "val");
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("newKey", "val").build());
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PopertiesWrite5OfEach() throws Exception {
    DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(10);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(original.getMessage(), original);
      for (int j = 1; j <= 5; j++) {
        event.setFlowVariable("newKey" + j, "val");
        event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("newKey" + j, "val").build());
      }
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(50);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(original.getMessage(), original);
      event.setFlowVariable("newKey", "val");
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("newKey", "val").build());
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(50);
    for (int i = 0; i < 1000; i++) {
      event = new DefaultMuleEvent(original.getMessage(), original);
      for (int j = 1; j <= 25; j++) {
        event.setFlowVariable("newKey" + j, "val");
        event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("newKey" + j, "val").build());
      }
    }
  }

  protected DefaultMuleEvent createMuleEvent() throws Exception {
    return new DefaultMuleEvent(MuleMessage.builder().payload(payload).build(), ONE_WAY, flow);
  }

  protected DefaultMuleEvent createMuleEventWithFlowVarsAndProperties(int numProperties) throws Exception {
    DefaultMuleEvent event = createMuleEvent();
    for (int i = 1; i <= numProperties; i++) {
      event.setFlowVariable("InBoUnDpRoPeRtYkEy" + i, "val");
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("InBoUnDpRoPeRtYkEy" + i, "val").build());
    }
    return event;
  }

}
