/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.impl.context.DeferredEndSpanWrapper;

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
  private DeferredEndSpanWrapper managedSpan;

  @Override
  public Span getParent() {
    return null;
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return SpanIdentifier.INVALID_SPAN_IDENTIFIER;
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
      managedSpan.doEndOriginalSpan();
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
  public int getAttributesCount() {
    return attributes.size();
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return emptyMap();
  }

  @Override
  public InternalSpan onChild(InternalSpan child) {
    if (!ROOT_SPAN.equals(name)) {
      child.updateRootName(name);
      attributes.forEach(child::setRootAttribute);
    }
    if (managedChildSpan && child instanceof ExportOnEndExecutionSpan) {
      if (managedSpan == null) {
        // The RootInternalSpan was still waiting for the managed span, then it wraps the child span to defer its end.
        ((ExportOnEndExecutionSpan) child).getSpanExporter().updateParentSpanFrom(serializeAsMap());
        managedSpan = new DeferredEndSpanWrapper(child);
        return managedSpan;
      } else {
        // The RootInternalSpan already has its managed span, then it delegates the call to it.
        return managedSpan.onChild(child);
      }
    } else {
      return child;
    }
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
