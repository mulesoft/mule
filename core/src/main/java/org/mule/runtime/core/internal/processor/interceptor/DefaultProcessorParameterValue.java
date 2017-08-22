/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.ProcessorParameterValue;

import java.util.function.Supplier;

final class DefaultProcessorParameterValue implements ProcessorParameterValue {

  private String name;
  private String provided;
  private Supplier<Object> resolver;

  DefaultProcessorParameterValue(String name, String provided, Supplier<Object> resolver) {
    this.name = name;
    this.provided = provided;
    this.resolver = resolver;
  }

  @Override
  public String parameterName() {
    return name;
  }

  @Override
  public String providedValue() {
    return provided;
  }

  @Override
  public Object resolveValue() throws MuleRuntimeException {
    return resolver.get();
  }

  @Override
  public String toString() {
    try {
      Object resolveValue = resolveValue();
      return format("ProcessorParameterValue{%s: %s -> %s}", parameterName(), providedValue(), resolveValue);
    } catch (Throwable t) {
      return format("ProcessorParameterValue{%s: %s -> Failed!: %s}", parameterName(), providedValue(), t.toString());
    }
  }
}
