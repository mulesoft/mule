/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Chain;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Extension of @{link Chain} that adds the feature to execute the chain with a given correlation id.
 * @since 4.4.0
 */
public interface ChildContextChain extends Chain {

  /**
   * Executes the chain of components starting with the same input message that it's container scope received,
   * setting the correlation id within the execution as the the one passed. The correlation id will return
   * to the previous values for the components following the chain execution.
   * <p>
   * {@code onSuccess} callback will be invoked with the output {@link Result} of the last component
   * in the {@link Chain} if no error occurred after all the components were executed exactly once.
   * <p>
   * {@code onError} callback will be invoked with the exception propagated by the first failing component,
   * along with the last output {@link Result} available. The given {@link Result} will be the same that was
   * used as input of the failing component.
   *
   * @param correlationId the correlation id to be used in the event within the execution of the chain
   * @param onSuccess the callback to be executed when a successful execution is completed by the {@link Chain}
   * @param onError  the callback to be executed when an error occurs during the execution
   *                 of the {@link Chain} components
   */
  void process(String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError);

}
