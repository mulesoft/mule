/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Optional;

/**
 * State handler for policies execution.
 *
 * Keeps track of the operation associated with a certain context of execution. Such context of execution is defined by the unique
 * identifier of the generated {@link CoreEvent}.
 * 
 * Implementations will be executed concurrently but always using different identifiers. There will be no concurrent invocation
 * for the same identifier.
 *
 * @since 4.0
 */
public interface PolicyStateHandler {

  /**
   * Associated the {@code identifier} with the policy next operation to execute
   * 
   * @param executionIdentifier the identifier of the context
   * @param nextOperation the next operation of the policy
   */
  void updateNextOperation(String executionIdentifier, Processor nextOperation);

  /**
   * @param executionIdentifier the identifier of the context
   * @return the next operation for the context.
   */
  Processor retrieveNextOperation(String executionIdentifier);

  /**
   * Frees resources associated with the given context identifier
   *
   * @param executionIdentifier the identifier of the context
   */
  void destroyState(String executionIdentifier);

  /**
   * @param identifier the identifier of the context
   * @return the latest state of the policy for the given identifier. It may be empty if no other policy was executed before for
   *         this context.
   */
  Optional<CoreEvent> getLatestState(PolicyStateId identifier);

  /**
   * Updates the event of the policy for the context with the given identifier.
   * 
   * @param identifier the identifier of the context
   * @param lastStateEvent the last state of the event
   */
  void updateState(PolicyStateId identifier, CoreEvent lastStateEvent);

}
