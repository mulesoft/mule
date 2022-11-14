/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getComponentNameWithoutNamespace;

/**
 * A {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo} that does not export the children of execute next.
 *
 * @since 4.5.0
 */
public class NoExportExecuteNextChildSpanCustomizationInfo extends NamedSpanBasedOnParentSpanChildSpanCustomizationInfo {

  private static final SpanCustomizationInfo POLICY_SPAN_INSTANCE = new NoExportExecuteNextChildSpanCustomizationInfo(true);

  private static final SpanCustomizationInfo NO_POLICY_SPAN_INSTANCE = new NoExportExecuteNextChildSpanCustomizationInfo(false);

  private final boolean policySpan;

  private NoExportExecuteNextChildSpanCustomizationInfo(boolean policySpan) {
    this.policySpan = policySpan;
  }

  /**
   * @param policySpan if it is a policy span
   * @return the {@link SpanCustomizationInfo}
   */
  public static SpanCustomizationInfo getCustomizationInfo(boolean policySpan) {
    if (policySpan) {
      return POLICY_SPAN_INSTANCE;
    } else {
      return NO_POLICY_SPAN_INSTANCE;
    }
  }

  @Override
  public boolean isExportable(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> !getComponentNameWithoutNamespace(internalSpan).equals(EXECUTE_NEXT_ROUTE_TAG)).orElse(true);
  }

  @Override
  public boolean isPolicySpan() {
    return policySpan;
  }
}
