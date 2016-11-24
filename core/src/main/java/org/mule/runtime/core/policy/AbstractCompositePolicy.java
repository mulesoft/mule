/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Optional;

/**
 * Abstract implementation that performs the chaining of a set of policies and the
 * {@link Processor} being intercepted.
 *
 * @param <ParametersTransformer> the type of the function parameters transformer.
 * @param <ParametersProcessor> the type of the parameters processor that provides access to the initial value of the parameters.
 *
 * @since 4.0
 */
public abstract class AbstractCompositePolicy<ParametersTransformer, ParametersProcessor> {

  private List<Policy> parameterizedPolicies;
  private Optional<ParametersTransformer> parametersTransformer;

  /**
   * Creates a new composite policy.
   *
   * @param policies list of {@link Policy} to chain together.
   * @param parametersTransformer transformer from the operation parameters to a message and vice versa.
   */
  public AbstractCompositePolicy(List<Policy> policies,
                                 Optional<ParametersTransformer> parametersTransformer) {
    checkArgument(!policies.isEmpty(), "policies list cannot be empty");
    this.parameterizedPolicies = policies;
    this.parametersTransformer = parametersTransformer;
  }

  /**
   * When this policy is processed, it will use the {@link CompositeOperationPolicy.NextOperationCall} which will keep track of
   * the different policies to be applied and the current index of the policy under execution.
   * <p>
   * The first time, the first policy in the {@code parameterizedPolicies} will be executed, it will receive as it next operation
   * the same instance of {@link CompositeOperationPolicy.NextOperationCall}, and since
   * {@link CompositeOperationPolicy.NextOperationCall} keeps track of the policy executed, it will execute the following policy
   * in the chain until the finally policy it's executed in which case then next operation of it, it will be the operation
   * execution.
   */
  public Event process(Event operationEvent, Processor nextProcessor, ParametersProcessor parametersProcessor)
      throws Exception {
    return new AbstractCompositePolicy.NextOperationCall(operationEvent, nextProcessor, parametersProcessor)
        .process(operationEvent);
  }

  /**
   * Template method for executing the final processor of the chain.
   * 
   * @param nextOperation the final processor of the chain.
   * @param event the event to use for executing the next operation.
   * @return the event to use for processing the after phase of the policy
   * @throws MuleException if there's an error executing processing the next operation.
   */
  protected abstract Event processNextOperation(Processor nextOperation, Event event) throws MuleException;

  /**
   * Template method for executing a policy.
   * 
   * @param policy the policy to execute
   * @param nextProcessor the next processor to use as the {@link PolicyNextActionMessageProcessor}. It will invoke the next
   *        policy in the chain.
   * @param parametersTransformer parameters transformer for the source response function or the operation function.
   * @param parametersProcessor the parameters processor that allows to get the initial values of the source response function
   *        parameters or the operation function parameters.
   * @param event the event to use for processing the policy.
   * @return the result to use for the next policy in the chain.
   * @throws Exception if the execution of the policy fails.
   */
  protected abstract Event processPolicy(Policy policy, Processor nextProcessor,
                                         Optional<ParametersTransformer> parametersTransformer,
                                         ParametersProcessor parametersProcessor,
                                         Event event)
      throws Exception;

  /**
   * Inner class that implements the actually chaining of policies.
   */
  public class NextOperationCall implements Processor {

    private final Event originalEvent;
    private final ParametersProcessor parametersProcessor;
    private int index = 0;
    private Processor nextProcessor;

    public NextOperationCall(Event originalEvent, Processor nextProcessor, ParametersProcessor parametersProcessor) {
      this.nextProcessor = nextProcessor;
      this.originalEvent = originalEvent;
      this.parametersProcessor = parametersProcessor;
    }

    @Override
    public Event process(Event event) throws MuleException {
      checkState(index <= parameterizedPolicies.size(), "composite policy index is greater that the number of policies.");
      if (index == parameterizedPolicies.size()) {
        return processNextOperation(nextProcessor, event);
      }
      Policy policy = parameterizedPolicies.get(index);
      index++;
      try {
        return processPolicy(policy, this, parametersTransformer, parametersProcessor, originalEvent);
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }



}
