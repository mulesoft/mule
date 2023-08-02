/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.NoopCoreEventTracer;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.client.NullComponent;
import org.mule.runtime.module.extension.internal.runtime.parameter.ImmutableCorrelationInfo;

import static org.mule.runtime.core.internal.profiling.NoopCoreEventTracer.getNoopCoreEventTracer;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;

/**
 * {@link ArgumentResolver} that yields instances of {@link CorrelationInfo}
 *
 * @since 4.1
 */
public class CorrelationInfoArgumentResolver implements ArgumentResolver<CorrelationInfo> {

  @Override
  public CorrelationInfo resolve(ExecutionContext executionContext) {
    CoreEvent event = ((ExecutionContextAdapter) executionContext).getEvent();
    if (((ExecutionContextAdapter<?>) executionContext).getComponent().equals(NULL_COMPONENT)) {
      // TODO: W-13837896
      // If this was executed from a null component don't allow to execute any tracing
      // from the connector. This is especially used in the cases where an extension in an
      // operation invokes another extension through an ExtensionsClient.
      return new ImmutableCorrelationInfo(event.getContext().getId(), true, event.getCorrelationId(),
                                          event.getItemSequenceInfo().orElse(null), event, getNoopCoreEventTracer());
    }
    return new ImmutableCorrelationInfo(event.getContext().getId(), true, event.getCorrelationId(),
                                        event.getItemSequenceInfo().orElse(null), event);
  }
}
