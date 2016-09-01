/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.el.mvel;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Random;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class MVELDeepInvokePerformanceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Override
  public int getTestTimeoutSecs() {
    return 180;
  }

  final protected String mel = "payload.setFirstName('Tom');"
      + "payload.setLastName('Fennelly');"
      + "payload.contact.setAddress('Male');"
      + "payload.contact.setTelnum('4');"
      + "payload.setSin('Ireland');"
      + "payload;";

  final protected Payload payload = new Payload();

  protected MuleEvent event;
  protected Flow flow;

  @Before
  public void before() throws Exception {
    ((MVELExpressionLanguage) muleContext.getExpressionLanguage()).setAutoResolveVariables(false);
    event = createMuleEvent();
    flow = getTestFlow();
    // Warmup
    for (int i = 0; i < 5000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event, flow);
    }
  }

  /**
   * Cold start: - New expression for each iteration - New context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(average = 630, percentile90 = 700)
  public void mvelColdStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel + new Random().nextInt(), createMuleEvent(), flow);
    }
  }

  /**
   * Warm start: - Same expression for each iteration - New context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(throughput = 750, average = 2, percentile90 = 3)
  public void mvelWarmStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event, flow);
    }
  }

  /**
   * Hot start: - Same expression for each iteration - Same context (message) for each iteration
   */
  @Test
  @PerfTest(duration = 30000, threads = 1, warmUp = 10000)
  @Required(throughput = 750, average = 2, percentile90 = 3)
  public void mvelHotStart() {
    for (int i = 0; i < 1000; i++) {
      muleContext.getExpressionLanguage().evaluate(mel, event, flow);
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
    Flow flow;
    try {
      flow = getTestFlow();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload(payload).build()).exchangePattern(ONE_WAY).flow(flow).build();
  }

  public static class Payload {

    public String firstName;
    public String lastName;
    public Contact contact = new Contact();
    public String sin;

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public String getSin() {
      return sin;
    }

    public void setSin(String sin) {
      this.sin = sin;
    }
  }

  public static class Contact {

    public String address;
    public String telnum;

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public String getTelnum() {
      return telnum;
    }

    public void setTelnum(String telnum) {
      this.telnum = telnum;
    }
  }
}
