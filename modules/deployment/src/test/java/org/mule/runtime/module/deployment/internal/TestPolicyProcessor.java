/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component used on deployment test that require policies to check that they are invoked
 * <p/>
 * Static state must be reset before each test is executed
 */
public class TestPolicyProcessor extends AbstractComponent implements org.mule.runtime.core.api.processor.Processor {

  public static volatile int invocationCount;
  public static volatile Map<String, AtomicInteger> correlationIdCount = new ConcurrentHashMap<>();
  public static volatile String policyParametrization = "";

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    invocationCount++;
    String variableName = "policyParameter";
    if (event.getVariables().keySet().contains(variableName)) {
      policyParametrization += event.getVariables().get(variableName).getValue();
    }

    correlationIdCount.computeIfAbsent(event.getCorrelationId(), k -> new AtomicInteger(0)).addAndGet(1);

    return event;
  }
}
