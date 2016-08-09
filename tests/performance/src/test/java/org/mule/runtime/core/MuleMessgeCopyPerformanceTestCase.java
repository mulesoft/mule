/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

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
public class MuleMessgeCopyPerformanceTestCase extends AbstractMuleTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Mock
  private MuleContext muleContext;

  private String payload;

  private MuleMessage message;

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
  public void copy() {
    MuleMessage original = createMuleMessage();
    for (int i = 0; i < 1000; i++) {
      message = MuleMessage.builder(original).build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20Poperties() {
    MuleMessage original = createMuleMessageWithProperties(10);
    for (int i = 0; i < 1000; i++) {
      message = MuleMessage.builder(original).build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100Poperties() {
    MuleMessage original = createMuleMessageWithProperties(50);
    for (int i = 0; i < 1000; i++) {
      message = MuleMessage.builder(original).build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20PopertiesWrite1Outbound() {
    MuleMessage original = createMuleMessageWithProperties(10);
    for (int i = 0; i < 1000; i++) {
      message = MuleMessage.builder(original).addOutboundProperty("newKey", "val").build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith20PopertiesWrite10Outbound() {
    MuleMessage original = createMuleMessageWithProperties(10);
    for (int i = 0; i < 1000; i++) {
      final Builder builder = MuleMessage.builder(original);
      for (int j = 1; j <= 10; j++) {
        builder.addOutboundProperty("newKey" + i, "val");
      }
      message = builder.build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100PopertiesWrite1Outbound() {
    MuleMessage original = createMuleMessageWithProperties(50);
    for (int i = 0; i < 1000; i++) {
      message = MuleMessage.builder(original).addOutboundProperty("newKey", "val").build();
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copyWith100PopertiesWrite50Outbound() {
    MuleMessage original = createMuleMessageWithProperties(50);
    for (int i = 0; i < 1000; i++) {
      final Builder builder = MuleMessage.builder(original);
      for (int j = 1; j <= 50; j++) {
        builder.addOutboundProperty("newKey" + i, "val");
      }
      message = builder.build();
    }
  }

  protected MuleMessage createMuleMessage() {
    return MuleMessage.builder().payload(payload).build();
  }

  protected MuleMessage createMuleMessageWithProperties(int numProperties) {
    final Builder builder = MuleMessage.builder().payload(payload);
    for (int i = 1; i <= numProperties; i++) {
      builder.addOutboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
    }
    for (int i = 1; i <= numProperties; i++) {
      builder.addOutboundProperty("OuTBoUnDpRoPeRtYkEy" + i, "val");
    }
    return builder.build();
  }

}
