/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test message processor to keep count of number of invocations.
 */
public class InvocationCountMessageProcessor extends AbstractComponent implements Processor, Initialisable {

  private static Map<String, AtomicInteger> invocationCountPerMessageProcessor = new HashMap<>();
  private final AtomicInteger invocationCount = new AtomicInteger();
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
    this.invocationCount.set(0);
    this.invocationCountPerMessageProcessor.put(this.name, this.invocationCount);
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
