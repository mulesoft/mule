/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import java.util.Map;

import org.mule.runtime.core.api.event.CoreEvent;

import org.reactivestreams.Publisher;

/**
 * Function for executing an operation.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface OperationExecutionFunction {

  /**
   * Executes the operation being intercepted by a policy.
   *
   * @param parameters the set of parameters required to execute the function.
   * @param operationEvent the event to use for executing the operation.
   * @return an {@link CoreEvent} as result of the operation execution.
   */
  Publisher<CoreEvent> execute(Map<String, Object> parameters, CoreEvent operationEvent);

}
