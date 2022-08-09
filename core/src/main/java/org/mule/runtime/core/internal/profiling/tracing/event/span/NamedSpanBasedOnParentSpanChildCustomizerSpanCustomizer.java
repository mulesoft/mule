/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanInfo.getDefaultChildSpanInfo;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanInfo;

import java.util.Optional;

/**
 * A {@link org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer} that sets the name based on the parent span's
 * {@link ChildSpanInfo}.
 *
 * @since 4.5.0
 */
public class NamedSpanBasedOnParentSpanChildCustomizerSpanCustomizer extends AbstractDefaultAttributesResolvingSpanCustomizer {

  @Override
  public String getName(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> internalSpan.getName() + internalSpan.getChildSpanInfo().getChildSpanSuggestedName()).orElse("");
  }

  @Override
  public ChildSpanInfo getChildSpanCustomizer() {
    return getDefaultChildSpanInfo();
  }

  private Optional<InternalSpan> getSpan(CoreEvent coreEvent) {
    return ((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan();
  }

  @Override
  public String getLocationAsString(CoreEvent coreEvent) {
    return getSpan(coreEvent).map(internalSpan -> internalSpan.getAttribute(LOCATION_KEY).orElse("")).orElse("");
  }
}
