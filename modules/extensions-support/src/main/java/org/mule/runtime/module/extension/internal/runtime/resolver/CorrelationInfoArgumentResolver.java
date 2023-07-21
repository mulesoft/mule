/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.ImmutableCorrelationInfo;

/**
 * {@link ArgumentResolver} that yields instances of {@link CorrelationInfo}
 *
 * @since 4.1
 */
public class CorrelationInfoArgumentResolver implements ArgumentResolver<CorrelationInfo> {

  @Override
  public CorrelationInfo resolve(ExecutionContext executionContext) {
    CoreEvent event = ((ExecutionContextAdapter) executionContext).getEvent();
    return new ImmutableCorrelationInfo(event.getContext().getId(), true, event.getCorrelationId(),
                                        event.getItemSequenceInfo().orElse(null), event);
  }
}
