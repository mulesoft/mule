/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizer.getDefaultChildCustomizer;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.ARTIFACT_ID_KEY;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.ARTIFACT_TYPE_ID;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.CORRELATION_ID_KEY;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.LOCATION_KEY;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.THREAD_START_ID_KEY;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanCustomizer.THREAD_START_NAME_KEY;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizer;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageProcessorChainSpanCustomizer implements SpanCustomizer {

  @Override
  public String getName(CoreEvent coreEvent) {
    return getSpan(coreEvent)
        .map(internalSpan -> internalSpan.getName() + internalSpan.getChildSpanCustomizer().getChildSpanName()).orElse("");
  }

  @Override
  public Map<String, String> getAttributes(CoreEvent coreEvent, MuleConfiguration muleConfiguration, ArtifactType artifactType) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(LOCATION_KEY,
                   getSpan(coreEvent).map(internalSpan -> internalSpan.getAttribute(LOCATION_KEY).orElse("")).orElse(""));
    attributes.put(CORRELATION_ID_KEY, coreEvent.getCorrelationId());
    attributes.put(ARTIFACT_ID_KEY, muleConfiguration.getId());
    attributes.put(ARTIFACT_TYPE_ID, artifactType.getAsString());
    attributes.put(THREAD_START_ID_KEY, Long.toString(Thread.currentThread().getId()));
    attributes.put(THREAD_START_NAME_KEY, Thread.currentThread().getName());
    addLogggingVariablesAsAttributes(coreEvent, attributes);
    return attributes;
  }

  @Override
  public ChildSpanCustomizer getChildSpanCustomizer() {
    return getDefaultChildCustomizer();
  }

  private Optional<InternalSpan> getSpan(CoreEvent coreEvent) {
    return ((DistributedTraceContextAware) coreEvent.getContext()).getDistributedTraceContext().getCurrentSpan();
  }

  private void addLogggingVariablesAsAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    if (coreEvent instanceof PrivilegedEvent) {
      Optional<Map<String, String>> loggingVariables = ((PrivilegedEvent) coreEvent).getLoggingVariables();
      if (loggingVariables.isPresent()) {
        for (Map.Entry<String, String> entry : ((PrivilegedEvent) coreEvent).getLoggingVariables().get().entrySet()) {
          attributes.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }
}
