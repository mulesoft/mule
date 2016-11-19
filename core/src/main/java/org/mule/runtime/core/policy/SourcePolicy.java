/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;

import java.io.Serializable;

/**
 * A {@code SourcePolicy} is responsible to handle the state of a policy applied to a particular execution.
 *
 * @since 4.0
 */
public interface SourcePolicy extends Serializable {

  /**
   * Process the source policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed
   * by the next-operation of the chain which may be the next policy in the chain or the flow execution.
   *
   *
   * @param sourceEvent the event with the data created from the source message that must be used to execute the source policy.
   * @param nextOperation the next-operation processor implementation, it may be another policy or the flow execution.
   * @param messageSourceResponseParametersProcessor a processor to convert an {@link Event} to the set of parameters used to
   *        execute the successful or failure response function of the source.
   * @return the result of processing the {@code event} through the policy chain.
   * @throws Exception
   */
  Event process(Event sourceEvent, NextOperation nextOperation,
                MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor)
      throws Exception;

}
