/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporterVisitor;

import java.util.Map;

/**
 * An {@link InternalSpanExporter} that does not perform any action.
 *
 * @since 4.5.0
 */
public class NoOpInternalSpanExporter implements InternalSpanExporter {

  private static final InternalSpanExporter INSTANCE = new NoOpInternalSpanExporter();

  public static InternalSpanExporter getNoExportInternalSpanExporter() {
    return INSTANCE;
  }

  private NoOpInternalSpanExporter() {

  }

  @Override
  public void export(InternalSpan internalSpan) {
    // Nothing to do.
  }

  @Override
  public <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor) {
    return internalSpanExporterVisitor.accept(this);
  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    // Nothing to do.
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    // Nothing to do.
  }

  @Override
  public void setCurrentName(String name) {
    // Nothing to do.
  }
}
