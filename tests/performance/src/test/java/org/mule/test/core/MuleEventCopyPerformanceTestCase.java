/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleEventCopyPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  private String payload;
  private Event events[];
  private Event muleEventWith0Properties;
  private Event muleEventWith10Properties;
  private Event muleEventWith50Properties;
  private Event muleEventWith100Properties;
  private static final int repetitions = 1000;

  @Before
  public void before() throws IOException {
    payload = IOUtils.getResourceAsString("test-data.json", getClass());
    events = new Event[repetitions];
    muleEventWith0Properties = createMuleEvent(InternalMessage.builder().payload(payload).build(), 0);
    muleEventWith10Properties = createMuleEventWithFlowVarsAndProperties(10);
    muleEventWith50Properties = createMuleEventWithFlowVarsAndProperties(50);
    muleEventWith100Properties = createMuleEventWithFlowVarsAndProperties(100);
  }

  @Test
  @Required(throughput = 1200, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copy() throws Exception {
    Event original = muleEventWith0Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = Event.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 1200, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10Properties() throws Exception {
    Event original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = Event.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 1100, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50Properties() throws Exception {
    Event original = muleEventWith50Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = Event.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 340, average = 3, percentile90 = 4)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    Event original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = Event.builder(original).session(new DefaultMuleSession(original.getSession()))
          .addVariable("newKey", "val")
          .message(InternalMessage.builder(original.getMessage()).addInboundProperty("newKey", "val")
              .addOutboundProperty("newKey", "val").build())
          .build();
    }
  }

  @Test
  @Required(throughput = 170, average = 6, percentile90 = 7)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    Event original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      final Builder eventBuilder = Event.builder(original);
      eventBuilder.session(new DefaultMuleSession(original.getSession())).build();
      InternalMessage.Builder builder = InternalMessage.builder(original.getMessage());
      for (int j = 1; j <= 5; j++) {
        eventBuilder.addVariable("newKey" + j, "val");
        builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
      }
      events[i] = eventBuilder.message(builder.build()).build();
    }
  }

  @Test
  @Required(throughput = 120, average = 8, percentile90 = 9)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    Event original = muleEventWith50Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = Event.builder(original).session(new DefaultMuleSession(original.getSession()))
          .addVariable("newKey", "val")
          .message(InternalMessage.builder(original.getMessage()).addInboundProperty("newKey", "val")
              .addOutboundProperty("newKey", "val").build())
          .build();
    }
  }

  @Test
  @Required(throughput = 35, average = 27, percentile90 = 30)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    Event original = muleEventWith100Properties;
    for (int i = 0; i < repetitions; i++) {
      final Builder eventBuilder = Event.builder(original);
      eventBuilder.session(new DefaultMuleSession(original.getSession())).build();
      InternalMessage.Builder builder = InternalMessage.builder(original.getMessage());
      for (int j = 1; j <= 25; j++) {
        eventBuilder.addVariable("newKey" + j, "val");
        builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
      }
      events[i] = eventBuilder.message(builder.build()).build();
    }
  }

  protected Event createMuleEvent(InternalMessage message, int numProperties) {
    final Event.Builder builder;
    try {
      builder = eventBuilder().message(message).exchangePattern(ONE_WAY);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addVariable("FlOwVaRiAbLeKeY" + i, "val");
    }
    return builder.build();
  }

  protected Event createMuleEventWithFlowVarsAndProperties(int numProperties) {
    InternalMessage.Builder builder = InternalMessage.builder().payload(payload);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    InternalMessage message = builder.build();
    Event event = createMuleEvent(message, numProperties);
    return event;
  }

}
