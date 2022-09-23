/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo.ROUTE_TAG;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo.SPAN_NAME_SEPARATOR;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.EXECUTE_NEXT;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getComponentNameWithoutNamespace;
import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import java.util.Optional;

/**
 * A {@link SpanCustomizationInfo} that sets the name based on the parent span's {@link ChildSpanCustomizationInfo}.
 *
 * @since 4.5.0
 */
public class NamedSpanBasedOnParentSpanChildSpanCustomizationInfo
    extends AbstractDefaultAttributesResolvingSpanCustomizationInfo {

  public static final String EXECUTE_NEXT_ROUTE_TAG = EXECUTE_NEXT + SPAN_NAME_SEPARATOR + ROUTE_TAG;

  @Override
  public String getName(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> internalSpan.getName() + internalSpan.getChildSpanInfo().getChildSpanSuggestedName()).orElse("");
  }

  @Override
  public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
    return getDefaultChildSpanInfo();
  }

  private Optional<InternalSpan> getSpan(CoreEvent coreEvent) {
    return ((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan();
  }

  @Override
  public String getLocationAsString(CoreEvent coreEvent) {
    return getSpan(coreEvent).map(internalSpan -> internalSpan.getAttribute(LOCATION_KEY).orElse("")).orElse("");
  }

  @Override
  public boolean isExportable(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> !getComponentNameWithoutNamespace(internalSpan).equals(EXECUTE_NEXT_ROUTE_TAG)).orElse(true);
  }
}
