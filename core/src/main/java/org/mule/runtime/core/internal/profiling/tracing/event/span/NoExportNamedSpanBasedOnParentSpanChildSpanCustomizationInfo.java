/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo} to not export the span.
 *
 * @since 4.5.0
 */
public class NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo
    extends NamedSpanBasedOnParentSpanChildSpanCustomizationInfo {

  private static final SpanCustomizationInfo INSTANCE = new NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo();
  private boolean policySpan = false;

  public static SpanCustomizationInfo getNoExportChildNamedSpanBasedOnParentSpanChildSpanCustomizationInfo() {
    return INSTANCE;
  }

  public static SpanCustomizationInfo getNoExportChildNamedSpanBasedOnParentSpanChildSpanCustomizationInfo(boolean policySpan) {
    return new NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo(policySpan);
  }

  private NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo() {}

  private NoExportNamedSpanBasedOnParentSpanChildSpanCustomizationInfo(boolean policySpan) {
    this.policySpan = policySpan;
  }

  @Override
  public boolean isExportable(CoreEvent coreEvent) {
    return false;
  }

  @Override
  public boolean isPolicySpan() {
    return policySpan;
  }
}
