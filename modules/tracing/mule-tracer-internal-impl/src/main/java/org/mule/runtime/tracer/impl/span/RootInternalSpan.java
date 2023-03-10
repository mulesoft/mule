/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.impl.context.NoExportOnEndInternalSpan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class RootInternalSpan implements InternalSpan {

  protected boolean managedChildSpan;

  public static final String ROOT_SPAN = "root";

  private String name = ROOT_SPAN;
  private Map<String, String> attributes = new HashMap<>();

  // This is a managed span that will not end.
  private NoExportOnEndInternalSpan managedSpan;

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
  public List<SpanError> getErrors() {
    return null;
  }

  @Override
  public boolean hasErrors() {
    return false;
  }

  @Override
  public void end() {
    if (managedSpan != null) {
      managedSpan.endDelegateSpan();
    }
  }

  @Override
  public void end(long endTime) {
    end();
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
  public int getAttributesCount() {
    return attributes.size();
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return emptyMap();
  }


  @Override
  public InternalSpan updateChildSpanExporter(InternalSpan internalSpan) {
    if (!ROOT_SPAN.equals(name)) {
      internalSpan.updateRootName(name);
      attributes.forEach(internalSpan::setRootAttribute);
    }

    if (managedChildSpan) {
      internalSpan.getSpanExporter().updateParentSpanFrom(serializeAsMap());
      managedSpan = new NoExportOnEndInternalSpan(internalSpan);
      return managedSpan;
    }

    return internalSpan;
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    attributes.forEach(biConsumer);
  }

  @Override
  public void addAttribute(String key, String value) {
    if (managedSpan != null) {
      managedSpan.addAttribute(key, value);
    }
    attributes.put(key, value);
  }
}
