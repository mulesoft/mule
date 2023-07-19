/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * {@link ArgumentResolver} which returns the {@link Error} in the {@link CoreEvent} associated to the given
 * {@link ExecutionContext}.
 * <p>
 * Notice that such {@link Error} could be {@code null}
 *
 * @since 4.0
 */
public final class ErrorArgumentResolver implements ArgumentResolver<Error> {

  @Override
  public Error resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter) executionContext).getEvent().getError().orElse(null);
  }
}
