/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.tracer.api.span.info.StartExportInfo;

import java.util.HashSet;
import java.util.Set;


public class NoExportableTillStartExportInfo implements StartExportInfo {

  private Set<String> resetSpans = new HashSet<>();
  private boolean exportable;

  public NoExportableTillStartExportInfo(String resetSpan, boolean exportable) {
    this.resetSpans.add(resetSpan);
    this.exportable = exportable;
  }

  public NoExportableTillStartExportInfo(Set<String> resetSpans, boolean exportable) {
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
