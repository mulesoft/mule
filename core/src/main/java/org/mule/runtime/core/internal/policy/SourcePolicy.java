/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

/**
 * Interceptor of a {@link Processor} that executes logic before and after it. It allows to modify the content of the response (if
 * any) to be sent by a {@link org.mule.runtime.core.api.source.MessageSource}
 *
 * @since 4.0
 */
public interface SourcePolicy {

  /**
   * Process the source policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed
   * by the next-operation of the chain which may be the next policy in the chain or the flow execution.
   *
   *
   * @param sourceEvent the event with the data created from the source message that must be used to execute the source policy.
   *        execute the successful or failure response function of the source.
   * @return the result of processing the {@code event} through the policy chain.
   */
  Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent);

}
