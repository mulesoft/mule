/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.export;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link InitialExportInfo} that indicates that the span shouldn't be exported until a span with certain names is found.
 *
 * @since 4.5.0
 */
public class NoExportTillSpanWithNameInitialExportInfo implements InitialExportInfo {

  private Set<String> resetSpans = new HashSet<>();
  private boolean exportable;

  public NoExportTillSpanWithNameInitialExportInfo(String resetSpan, boolean exportable) {
    this.resetSpans.add(resetSpan);
    this.exportable = exportable;
  }

  public NoExportTillSpanWithNameInitialExportInfo(Set<String> resetSpans, boolean exportable) {
    this.resetSpans.addAll(resetSpans);
    this.exportable = exportable;
  }

  @Override
  public Set<String> noExportUntil() {
    return resetSpans;
  }

  @Override
  public boolean isExportable() {
    return exportable;
  }
}
