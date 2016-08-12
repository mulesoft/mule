/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultMessageExecutionContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Random;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class MVELPerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Override
  public int getTestTimeoutSecs() {
    return 180;
  }

  final protected String mel =
      "StringBuilder sb = new StringBuilder(); fields = payload.split(',\');" + "if (fields.length > 4) {"
          + "    sb.append('  <Contact>\n');" + "    sb.append('    <FirstName>').append(fields[0]).append('</FirstName>\n');"
          + "    sb.append('    <LastName>').append(fields[1]).append('</LastName>\n');"
          + "    sb.append('    <Address>').append(fields[2]).append('</Address>\n');"
          + "    sb.append('    <TelNum>').append(fields[3]).append('</TelNum>\n');"
          + "    sb.append('    <SIN>').append(fields[4]).append('</SIN>\n');" + "    sb.append('  </Contact>\n');" + "}"
          + "sb.toString();";

  final protected String payload = "Tom,Fennelly,Male,4,Ireland";

  protected MuleEvent event;

  @Before
  public void before() {
    ((MVELExpressionLanguage) muleContext.getExpressionLanguage()).setAutoResolveVariables(false);
    event = createMuleEvent();
    // Warmup
    for (int i = 0; i < 5000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event);
    }
  }

  /**
   * Cold start: - New expression for each iteration - New context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(median = 1000)
  public void mvelColdStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel + new Random().nextInt(), createMuleEvent());
    }
  }

  /**
   * Warm start: - Same expression for each iteration - New context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(median = 25)
  public void mvelWarmStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event);
    }
  }

  /**
   * Hot start: - Same expression for each iteration - Same context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(median = 25)
  public void mvelHotStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event);
    }
  }

  @Ignore
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  public void createEventBaseline() {
    for (int i = 0; i < 1000; i++) {
      createMuleEvent();
    }
  }

  protected MuleEvent createMuleEvent() {
    return new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                MuleMessage.builder().payload(payload).build(), ONE_WAY, (Flow) null);
  }

}
