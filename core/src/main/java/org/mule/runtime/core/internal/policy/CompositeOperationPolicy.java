/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.empty;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * {@link OperationPolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with operation policies.
 *
 * @since 4.0
 */
public class CompositeOperationPolicy extends
    AbstractCompositePolicy<OperationPolicyParametersTransformer, OperationParametersProcessor> implements OperationPolicy {


  private final Processor nextOperation;
  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private CoreEvent nextOperationResponse;

  /**
   * Creates a new composite policy.
   *
   * If a non-empty {@code operationPolicyParametersTransformer} is passed to this class, then it will be used to convert the flow
   * execution response parameters to a message with the content of such parameters in order to allow the pipeline after the
   * next-operation to modify the response. If an empty {@code operationPolicyParametersTransformer} is provided then the policy
   * won't be able to change the response parameters of the source and the original response parameters generated from the source
   * will be usd.
   * 
   * @param parameterizedPolicies list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param operationPolicyProcessorFactory factory for creating each {@link OperationPolicy} from a {@link Policy}
   * @param operationExecutionFunction the function that executes the operation.
   */
  public CompositeOperationPolicy(List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  OperationPolicyProcessorFactory operationPolicyProcessorFactory,
                                  OperationParametersProcessor operationParametersProcessor,
                                  OperationExecutionFunction operationExecutionFunction) {
    super(parameterizedPolicies, operationPolicyParametersTransformer, operationParametersProcessor);
    this.nextOperation = new Processor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return from(publisher).flatMap(event -> {
          Map<String, Object> parametersMap = new HashMap<>();
          try {
            parametersMap.putAll(operationParametersProcessor.getOperationParameters());
          } catch (Exception e) {
            return error(e);
          }
          if (operationPolicyParametersTransformer.isPresent()) {
            parametersMap
                .putAll(operationPolicyParametersTransformer.get().fromMessageToParameters(event.getMessage()));
          }
          return from(operationExecutionFunction.execute(parametersMap, event));
        });
      }
    };
    this.operationPolicyProcessorFactory = operationPolicyProcessorFactory;
  }

  /**
   * Stores the operation result so all the chains after the operation execution are executed with the actual operation result and
   * not a modified version from another policy.
   *
   * @param event the event to execute the operation.
   */
  @Override
  protected Publisher<CoreEvent> processNextOperation(CoreEvent event) {
    return just(event).transform(nextOperation).doOnNext(response -> {
      this.nextOperationResponse = response;
    });
  }

  /**
   * Always uses the stored result of {@code processNextOperation} so all the chains after the operation execution are executed
   * with the actual operation result and not a modified version from another policy.
   *
   * @param policy the policy to execute.
   * @param nextProcessor the processor to execute when the policy next-processor gets executed
   * @param event the event to use to execute the policy chain.
   */
  @Override
  protected Publisher<CoreEvent> processPolicy(Policy policy, Processor nextProcessor, CoreEvent event) {
    Processor defaultOperationPolicy =
        operationPolicyProcessorFactory.createOperationPolicy(policy, nextProcessor);
    return just(event).transform(defaultOperationPolicy).map(policyResponse -> nextOperationResponse);
  }

  @Override
  public Publisher<CoreEvent> process(CoreEvent operationEvent) {
    try {
      Message message = getParametersTransformer().isPresent()
          ? getParametersTransformer().get().fromParametersToMessage(getParametersProcessor().getOperationParameters())
          : operationEvent.getMessage();
      return processWithChildContext(CoreEvent.builder(operationEvent).message(message).build(), getPolicyProcessor(),
                                     empty());
    } catch (Exception e) {
      return error(e);
    }
  }
}
