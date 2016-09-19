/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleMessgeCopyPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  private static final int repetitions = 1000;
  private InternalMessage[] messages;
  private InternalMessage muleMessageWith0Properties;
  private InternalMessage muleMessageWith20Properties;
  private InternalMessage muleMessageWith100Properties;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private String payload;

  @Before
  public void before() throws IOException {
    payload = IOUtils.getResourceAsString("test-data.json", getClass());
    messages = new InternalMessage[repetitions];
    muleMessageWith0Properties = createMuleMessage();
    muleMessageWith20Properties = createMuleMessageWithProperties(20);
    muleMessageWith100Properties = createMuleMessageWithProperties(100);
  }

  @Test
  @Required(throughput = 14, average = 27, percentile90 = 30)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copy() {
    InternalMessage original = muleMessageWith0Properties;
    for (int i = 0; i < repetitions; i++) {
      messages[i] = InternalMessage.builder(original).build();
    }
  }

  @Test
  @Required(throughput = 14, average = 27, percentile90 = 30)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20Properties() {
    InternalMessage original = muleMessageWith20Properties;
    for (int i = 0; i < repetitions; i++) {
      messages[i] = InternalMessage.builder(original).build();
    }
  }

  @Test
  @Required(throughput = 14, average = 27, percentile90 = 30)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100Properties() {
    InternalMessage original = muleMessageWith100Properties;
    for (int i = 0; i < repetitions; i++) {
      messages[i] = InternalMessage.builder(original).build();
    }
  }

  @Test
  @Required(throughput = 18, average = 50, percentile90 = 75)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20PropertiesWrite1Outbound() {
    InternalMessage original = muleMessageWith20Properties;
    for (int i = 0; i < repetitions; i++) {
      InternalMessage.Builder builder = InternalMessage.builder(original);
      messages[i] = builder.addInboundProperty("newKey" + i, "val").build();
    }
  }

  @Test
  @Required(throughput = 14, average = 65, percentile90 = 70)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20PopertiesWrite100Outbound() {
    InternalMessage original = muleMessageWith20Properties;
    for (int i = 0; i < repetitions; i++) {
      InternalMessage.Builder builder = InternalMessage.builder(original);
      for (int j = 1; j <= 100; j++) {
        builder.addInboundProperty("newKey" + i, "val");
      }
      messages[i] = builder.build();
    }
  }

  @Test
  @Required(throughput = 15, average = 65, percentile90 = 70)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100PropertiesWrite1Outbound() {
    InternalMessage original = muleMessageWith100Properties;
    for (int i = 0; i < repetitions; i++) {
      InternalMessage.Builder builder = InternalMessage.builder(original);
      messages[i] = builder.addInboundProperty("newKey" + i, "val").build();
    }
  }

  @Test
  @Required(throughput = 15, average = 65, percentile90 = 70)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100PropertiesWrite50Outbound() {
    InternalMessage original = muleMessageWith100Properties;
    for (int i = 0; i < repetitions; i++) {
      InternalMessage.Builder builder = InternalMessage.builder(original);
      for (int j = 1; j <= 50; j++) {
        builder.addInboundProperty("newKey" + i, "val");
      }
      messages[i] = builder.build();
    }
  }

  protected InternalMessage createMuleMessage() {
    return InternalMessage.builder().payload(payload).build();
  }

  protected InternalMessage createMuleMessageWithProperties(int numProperties) {
    InternalMessage.Builder builder = InternalMessage.builder().payload(payload);
    for (int i = 1; i <= numProperties; i++) {
      builder.addInboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addOutboundProperty("OuTBoUnDpRoPeRtYkEy" + i, "val");
    }
    return builder.build();
  }

}
