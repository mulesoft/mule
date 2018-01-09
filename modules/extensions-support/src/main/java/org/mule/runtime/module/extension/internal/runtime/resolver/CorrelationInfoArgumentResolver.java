/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
                                        event.getItemSequenceInfo().orElse(null));
  }
}
