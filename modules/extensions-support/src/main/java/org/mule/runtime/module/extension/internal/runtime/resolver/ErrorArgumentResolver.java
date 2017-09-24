/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * {@link ArgumentResolver} which returns the {@link Error} in the {@link CoreEvent}
 * associated to the given {@link ExecutionContext}.
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
