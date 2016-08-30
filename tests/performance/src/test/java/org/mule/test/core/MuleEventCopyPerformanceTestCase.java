/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleEventCopyPerformanceTestCase extends AbstractMuleContextTestCase {


  private String payload;
  private MuleEvent events[];
  private MuleEvent muleEventWith0Properties;
  private MuleEvent muleEventWith10Properties;
  private MuleEvent muleEventWith50Properties;
  private MuleEvent muleEventWith100Properties;
  private static final int repetitions = 1000;

  @Override
  public int getTestTimeoutSecs() {
    return 120;
  }

  @Before
  public void before() throws IOException {
    payload = IOUtils.getResourceAsString("test-data.json", getClass());
    events = new MuleEvent[repetitions];
    muleEventWith0Properties = createMuleEvent(MuleMessage.builder().payload(payload).build(), 0);
    muleEventWith10Properties = createMuleEventWithFlowVarsAndProperties(10);
    muleEventWith50Properties = createMuleEventWithFlowVarsAndProperties(50);
    muleEventWith100Properties = createMuleEventWithFlowVarsAndProperties(100);
  }

  @Test
  @Required(throughput = 1200, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copy() throws Exception {
    MuleEvent original = muleEventWith0Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 1200, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10Properties() throws Exception {
    MuleEvent original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 1100, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50Properties() throws Exception {
    MuleEvent original = muleEventWith50Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
    }
  }

  @Test
  @Required(throughput = 340, average = 3, percentile90 = 4)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    MuleEvent original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
      events[i].setFlowVariable("newKey", "val");
      events[i].setMessage(MuleMessage.builder(events[i].getMessage()).addInboundProperty("newKey", "val")
          .addOutboundProperty("newKey", "val").build());
    }
  }

  @Test
  @Required(throughput = 170, average = 6, percentile90 = 7)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    MuleEvent original = muleEventWith10Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
      MuleMessage.Builder builder = MuleMessage.builder(events[i].getMessage());
      for (int j = 1; j <= 5; j++) {
        events[i].setFlowVariable("newKey" + j, "val");
        builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
      }
      events[i].setMessage(builder.build());
    }
  }

  @Test
  @Required(throughput = 120, average = 8, percentile90 = 9)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    MuleEvent original = muleEventWith50Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
      events[i].setFlowVariable("newKey", "val");
      events[i].setMessage(MuleMessage.builder(events[i].getMessage()).addInboundProperty("newKey", "val")
          .addOutboundProperty("newKey", "val").build());
    }
  }

  @Test
  @Required(throughput = 35, average = 27, percentile90 = 30)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    MuleEvent original = muleEventWith100Properties;
    for (int i = 0; i < repetitions; i++) {
      events[i] = (DefaultMuleEvent) MuleEvent.builder(original).session(new DefaultMuleSession(original.getSession())).build();
      MuleMessage.Builder builder = MuleMessage.builder(events[i].getMessage());
      for (int j = 1; j <= 25; j++) {
        events[i].setFlowVariable("newKey" + j, "val");
        builder.addInboundProperty("newKey", "val").addOutboundProperty("newKey", "val").build();
      }
      events[i].setMessage(builder.build());
    }
  }

  protected MuleEvent createMuleEvent(MuleMessage message, int numProperties) {
    Flow flow;
    try {
      flow = getTestFlow();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    final MuleEvent.Builder builder = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message)
        .exchangePattern(ONE_WAY).flow(flow);
    for (int i = 1; i <= numProperties; i++) {
      builder.addFlowVariable("FlOwVaRiAbLeKeY" + i, "val");
    }
    return builder.build();
  }

  protected MuleEvent createMuleEventWithFlowVarsAndProperties(int numProperties) {
    MuleMessage.Builder builder = MuleMessage.builder().payload(payload);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    MuleMessage message = builder.build();
    MuleEvent event = createMuleEvent(message, numProperties);
    return event;
  }

}
