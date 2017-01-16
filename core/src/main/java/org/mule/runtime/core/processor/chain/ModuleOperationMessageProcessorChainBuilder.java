/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.api.message.InternalMessage.builder;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.PARAM_VARS;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.PROPERTY_VARS;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.NotificationUtils;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;

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
 *    <set-payload value="#[mel:param.value1 + param.value2]"/>
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
      implements Processor, MessageProcessorContainer {

    private Map<String, String> properties;
    private Map<String, String> parameters;
    private boolean returnsVoid;
    private ExpressionManager expressionManager;

    ModuleOperationProcessorChain(String name, Processor head, List<Processor> processors,
                                  List<Processor> processorsForLifecycle,
                                  Map<String, String> properties, Map<String, String> parameters,
                                  boolean returnsVoid,
                                  ExpressionManager expressionManager) {
      super(name, head, processors, processorsForLifecycle);
      this.properties = properties;
      this.parameters = parameters;
      this.returnsVoid = returnsVoid;
      this.expressionManager = expressionManager;
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
      MessageProcessorPathElement subprocessors = pathElement.addChild(name).addChild("subprocessorsModuleOperations");
      NotificationUtils.addMessageProcessorPathElements(processors, subprocessors);
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

      if (!returnsVoid) {
        event = createNewEventFromJustMessage(event, eventResponse);
      }
      return event;
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      if (!returnsVoid) {
        return from(publisher)
            .concatMap(request -> just(request)
                .map(event -> createEventWithParameters(event))
                .transform(s -> super.apply(s))
                .map(result -> createNewEventFromJustMessage(request, result)));
      } else {
        return publisher;
      }
    }

    private Event createNewEventFromJustMessage(Event request, Event response) {
      return Event.builder(request).message(builder(response.getMessage()).build()).build();
    }

    private Event createEventWithParameters(Event event) {
      Event.Builder builder = Event.builder(event.getContext());
      builder.message(builder().nullPayload().build());
      properties.forEach(addEvaluatedParam(event, builder, PROPERTY_VARS));
      parameters.forEach(addEvaluatedParam(event, builder, PARAM_VARS));
      return builder.build();
    }

    private BiConsumer<String, String> addEvaluatedParam(Event event, Event.Builder builder, String prefix) {
      return (name, value) -> builder.addVariable(prefix + "." + name, expressionManager.isExpression(value)
          ? expressionManager.evaluate(value, event, flowConstruct)
          : value);

    }
  }
}
