/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorParameterValue;

import java.util.Map;

public final class LoadsOwnResourceInterceptor implements ProcessorInterceptor {

  @Override
  public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event) {
    ClassLoader tccl = currentThread().getContextClassLoader();

    if (tccl.getResource("test-resource.txt") == null) {
      throw new AssertionError("Couldn't load exported resource");
    }
    if (tccl.getResource("test-resource-not-exported.txt") == null) {
      throw new AssertionError("Couldn't load exported resource");
    }

    try {
      tccl.loadClass("org.bar1.BarUtils");
    } catch (ClassNotFoundException e) {
      throw new AssertionError("Couldn't load exported class", e);
    }
    try {
      tccl.loadClass("org.bar2.BarUtils");
    } catch (ClassNotFoundException e) {
      throw new AssertionError("Couldn't load exported class", e);
    }

  }
}