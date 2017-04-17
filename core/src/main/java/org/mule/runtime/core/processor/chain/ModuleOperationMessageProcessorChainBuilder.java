/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.internal.message.InternalMessage.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates a chain for any operation, where it parametrizes two type of values (parameter and property) to the inner processors
 * through the {@link Event}.
 *
 * <p>
 * Both parameter and property could be simple literals or expressions that will be evaluated before passing the new {@link Event}
 * to the child processors.
 *
 * <p>
 * Taking the following sample where the current event passed to {@link ModuleOperationProcessorChain#doProcess(Event)} has a flow
 * variable under "person" with a value of "stranger!", the result of executing the above processor will be "howdy stranger!":
 * 
 * <pre>
 *  <module-operation-chain returnsVoid="false">
 *    <module-operation-properties/>
 *    <module-operation-parameters>
 *      <module-operation-parameter-entry value="howdy" key="value1"/>
 *      <module-operation-parameter-entry value="#[mel:flowVars.person]" key="value2"/>
 *    </module-operation-parameters>
 *    <set-payload value="#[param.value1 ++ param.value2]"/>
 * </module-operation-chain>
 * </pre>
 */
public class ModuleOperationMessageProcessorChainBuilder extends ExplicitMessageProcessorChainBuilder {

  private Map<String, String> properties;
  private Map<String, String> parameters;
  private boolean returnsVoid;
  private ExpressionManager expressionManager;

  public ModuleOperationMessageProcessorChainBuilder(Map<String, String> properties,
                                                     Map<String, String> parameters, boolean returnsVoid,
                                                     ExpressionManager expressionManager) {
    this.properties = properties;
    this.parameters = parameters;
    this.returnsVoid = returnsVoid;
    this.expressionManager = expressionManager;
  }

  @Override
  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorForLifecycle) {
    return new ModuleOperationProcessorChain("wrapping-operation-module-chain", head, processors, processorForLifecycle,
                                             properties, parameters, returnsVoid,
                                             expressionManager);
  }

  /**
   * Generates message processor for a specific set of parameters & properties to be added in a new event.
   */
  static class ModuleOperationProcessorChain extends ExplicitMessageProcessorChain
      implements Processor {

    private Map<String, String> properties;
    private Map<String, String> parameters;
    private boolean returnsVoid;
    private ExpressionManager expressionManager;
    private Optional<String> target;

    ModuleOperationProcessorChain(String name, Processor head, List<Processor> processors,
                                  List<Processor> processorsForLifecycle,
                                  Map<String, String> properties, Map<String, String> parameters,
                                  boolean returnsVoid,
                                  ExpressionManager expressionManager) {
      super(name, head, processors, processorsForLifecycle);
      this.properties = properties;
      this.target = parameters.containsKey(TARGET_PARAMETER_NAME) ? of(parameters.remove(TARGET_PARAMETER_NAME)) : empty();
      this.parameters = parameters;
      this.returnsVoid = returnsVoid;
      this.expressionManager = expressionManager;
    }

    /**
     * Given an {@code event}, it will consume from it ONLY the defined properties and parameters that were set when initializing
     * this class to provide scoping for the inner list of processors.
     *
     * @param event parameter to consume elements from
     * @return a modified {@link Event} if the output of the operation was not void, the same event otherwise.
     * @throws MuleException
     */
    @Override
    protected Event doProcess(Event event) throws MuleException {
      Event eventResponse = super.doProcess(createEventWithParameters(event));
      return processResult(event, eventResponse);
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher)
          .concatMap(request -> just(request)
              .map(this::createEventWithParameters)
              .transform(super::apply)
              .map(eventResult -> processResult(request, eventResult)));
    }

    private Event processResult(Event originalEvent, Event chainEvent) {
      if (!returnsVoid) {
        originalEvent = createNewEventFromJustMessage(originalEvent, chainEvent);
      }
      return originalEvent;
    }

    private Event createNewEventFromJustMessage(Event request, Event response) {
      final Event.Builder builder = Event.builder(request);
      if (target.isPresent()) {
        builder.addVariable(target.get(), response.getMessage());
      } else {
        builder.message(builder(response.getMessage()).build());
      }
      return builder.build();
    }

    private Event createEventWithParameters(Event event) {
      Event.Builder builder = Event.builder(event.getContext());
      builder.message(builder().nullPayload().build());
      builder.parameters(evaluateParameters(event, parameters));
      builder.properties(evaluateParameters(event, properties));
      return builder.build();
    }

    private Map<String, Object> evaluateParameters(Event event, Map<String, String> unevaluatedMap) {
      return unevaluatedMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> getEvaluatedValue(event, entry.getValue())));
    }

    private Object getEvaluatedValue(Event event, String value) {
      return expressionManager.isExpression(value)
          ? expressionManager.evaluate(value, event, flowConstruct).getValue()
          : value;
    }
  }
}
