/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Component used on deployment test that require policies to check that they are invoked
 * <p/>
 * Static state must be reset before each test is executed
 */
public class TestPolicyProcessor implements org.mule.runtime.core.api.processor.Processor {

  public static volatile int invocationCount;
  public static volatile String policyParametrization = "";

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    invocationCount++;
    String variableName = "policyParameter";
    if (event.getVariables().keySet().contains(variableName)) {
      policyParametrization += event.getVariables().get(variableName).getValue();
    }

    return event;
  }
}
