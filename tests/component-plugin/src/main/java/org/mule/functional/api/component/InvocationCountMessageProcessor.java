/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test message processor to keep count of number of invocations.
 */
public class InvocationCountMessageProcessor extends AbstractComponent implements Processor, Initialisable, Startable {

  private static Map<String, AtomicInteger> invocationCountPerMessageProcessor = new HashMap<>();
  private AtomicInteger invocationCount;
  private String name;


  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    invocationCount.incrementAndGet();
    return event;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.invocationCount = invocationCountPerMessageProcessor.computeIfAbsent(this.name, k -> new AtomicInteger(0));
  }

  @Override
  public void start() throws MuleException {
    this.invocationCount.set(0);
  }

  /**
   * @param componentName name of the message processor in the configuration
   * @return the number of invocations for the message processor with name componentName
   */
  public static int getNumberOfInvocationsFor(String componentName) {
    AtomicInteger count = invocationCountPerMessageProcessor.get(componentName);
    if (count == null) {
      throw new IllegalArgumentException("No invocation-counter component registered under name: " + componentName
          + ", registered components: " + invocationCountPerMessageProcessor.keySet());
    }
    return count.get();
  }

}
