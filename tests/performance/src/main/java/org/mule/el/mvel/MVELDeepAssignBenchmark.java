/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.el.mvel;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public class MVELDeepAssignBenchmark extends AbstractBenchmark {

  final protected String mel = "mel:payload.firstName = 'Tom';"
      + "payload.lastName = 'Fennelly';"
      + "payload.contact.address = 'Male';"
      + "payload.contact.telnum = '4';"
      + "payload.sin = 'Ireland';"
      + "payload;";

  final protected Payload payload = new Payload();

  private MuleContext muleContext;
  private Flow flow;
  private CoreEvent event;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    MVELExpressionLanguage mvelExpressionLanguage =
        (MVELExpressionLanguage) lookupObject(muleContext, OBJECT_EXPRESSION_LANGUAGE);
    mvelExpressionLanguage.setAutoResolveVariables(false);

    ExtendedExpressionManager expressionManager = muleContext.getExpressionManager();
    ((DefaultExpressionManager) expressionManager).setMelDefault(true);
    ((DefaultExpressionManager) expressionManager).setExpressionLanguage(mvelExpressionLanguage);
    flow = createFlow(muleContext);
    event = createEvent(flow, payload);
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(lookupObject(muleContext, SchedulerService.class));
    muleContext.dispose();
  }

  /**
   * Cold start: - New expression for each iteration - New context (message) for each iteration
   */
  @Benchmark
  public Object mvelColdStart() {
    return muleContext.getExpressionManager().evaluate(mel + new Random().nextInt(), createEvent(flow, payload));
  }

  /**
   * Warm start: - Same expression for each iteration - New context (message) for each iteration
   */
  @Benchmark
  public Object mvelWarmStart() {
    return muleContext.getExpressionManager().evaluate(mel, event);
  }

  /**
   * Hot start: - Same expression for each iteration - Same context (message) for each iteration
   */
  @Benchmark
  public Object mvelHotStart() {
    return muleContext.getExpressionManager().evaluate(mel, event);
  }

  public static class Payload {

    public String firstName;
    public String lastName;
    public Contact contact = new Contact();
    public String sin;
  }

  public static class Contact {

    public String address;
    public String telnum;
  }
}
