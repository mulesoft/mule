/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.trace;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

public class RootInternalSpan implements InternalSpan {

  public static final String ROOT_SPAN = "root";

  private String name = ROOT_SPAN;
  private Map<String, String> attributes = new HashMap<>();

  @Override
  public Span getParent() {
    return null;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SpanDuration getDuration() {
    return null;
  }

  @Override
  public Set<SpanError> getErrors() {
    return null;
  }

  @Override
  public boolean hasErrors() {
    return false;
  }

  @Override
  public void end() {
    // Nothing to do.
  }

  @Override
  public void addError(InternalSpanError error) {
    // Nothing to do.
  }

  @Override
  public void updateName(String name) {
    this.name = name;
  }

  @Override
  public SpanExporter getSpanExporter() {
    return null;
  }

  @Override
  public Map<String, String> getAttributes() {
    return null;
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return emptyMap();
  }


  @Override
  public void updateChildSpanExporter(InternalSpan internalSpan) {
    if (!name.equals(ROOT_SPAN)) {
      internalSpan.updateRootName(name);
      attributes.forEach(internalSpan::setRootAttribute);
    }
  }

  @Override
  public void addAttribute(String key, String value) {
    attributes.put(key, value);
  }
}
