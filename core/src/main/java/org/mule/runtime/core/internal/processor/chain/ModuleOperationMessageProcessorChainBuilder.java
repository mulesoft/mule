/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.internal.message.InternalMessage.builder;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.getErrorMappings;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static reactor.core.publisher.Flux.from;

import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.utils.MetadataTypeUtils;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Creates a chain for any operation, where it parametrizes two type of values (parameter and property) to the inner processors
 * through the {@link CoreEvent}.
 *
 * <p>
 * Both parameter and property could be simple literals or expressions that will be evaluated before passing the new
 * {@link CoreEvent} to the child processors.
 *
 * <p>
 * Taking the following sample where the current event is processed using {@link ModuleOperationProcessorChain#apply(Publisher)},
 * has a variable under "person" with a value of "stranger!", the result of executing the above processor will be "howdy
 * stranger!":
 *
 * <pre>
 *  <module-operation-chain moduleName="a-module-name" moduleOperation="an-operation-name">
 *    <module-operation-properties/>
 *    <module-operation-parameters>
 *      <module-operation-parameter-entry value="howdy" key="value1"/>
 *      <module-operation-parameter-entry value="#[vars.person]" key="value2"/>
 *    </module-operation-parameters>
 *    <set-payload value="#[param.value1 ++ param.value2]"/>
 * </module-operation-chain>
 * </pre>
 */
public class ModuleOperationMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {

  /**
   * literal that represents the name of the global element for any given module. If the module's name is math, then the value of
   * this field will name the global element as <math:config ../>
   */
  public static final String MODULE_CONFIG_GLOBAL_ELEMENT_NAME = "config";

  /**
   * literal that represents the name of the connection element for any given module. If the module's name is github, then the
   * value of this field will name the global element as <github:connection ../>. As an example, think of the following snippet:
   *
   * <code>
   *    <github:config configParameter="someFood" ...>
   *      <github:connection username="myUsername" .../>
   *    </github:config>
   * </code>
   */
  public static final String MODULE_CONNECTION_GLOBAL_ELEMENT_NAME = "connection";

  private final Map<String, String> properties;
  private final Map<String, String> parameters;
  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final ExpressionManager expressionManager;

  public ModuleOperationMessageProcessorChainBuilder(Map<String, String> properties,
                                                     Map<String, String> parameters,
                                                     ExtensionModel extensionModel, OperationModel operationModel,
                                                     ExpressionManager expressionManager) {
    this.properties = properties;
    this.parameters = parameters;
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.expressionManager = expressionManager;
  }

  @Override
  protected MessageProcessorChain createSimpleChain(List<Processor> processors,
                                                    Optional<ProcessingStrategy> processingStrategyOptional) {
    return new ModuleOperationProcessorChain("wrapping-operation-module-chain",
                                             processors,
                                             properties, parameters,
                                             extensionModel, operationModel,
                                             expressionManager,
                                             processingStrategy);
  }

  /**
   * Generates message processor for a specific set of parameters & properties to be added in a new event.
   */
  public static class ModuleOperationProcessorChain extends DefaultMessageProcessorChain {

    private final Map<String, Pair<String, MetadataType>> properties;
    private final Map<String, Pair<String, MetadataType>> parameters;
    private final boolean returnsVoid;
    private final ExpressionManager expressionManager;
    private final Optional<String> target;
    private final String targetValue;

    ModuleOperationProcessorChain(String name, List<Processor> processors,
                                  Map<String, String> properties, Map<String, String> parameters,
                                  ExtensionModel extensionModel, OperationModel operationModel,
                                  ExpressionManager expressionManager,
                                  ProcessingStrategy processingStrategy) {
      super(name, ofNullable(processingStrategy), processors);
      final List<ParameterModel> propertiesModels = getAllProperties(extensionModel);
      this.properties = parseParameters(properties, propertiesModels);
      this.target = parameters.containsKey(TARGET_PARAMETER_NAME) ? of(parameters.remove(TARGET_PARAMETER_NAME)) : empty();
      this.targetValue = parameters.remove(TARGET_VALUE_PARAMETER_NAME);
      this.parameters = parseParameters(parameters, operationModel.getAllParameterModels());
      this.returnsVoid = MetadataTypeUtils.isVoid(operationModel.getOutput().getType());
      this.expressionManager = expressionManager;
    }

    /**
     * Plains the complete list of configurations and connections for the parameterized {@link ExtensionModel}
     *
     * @param extensionModel looks for all the the parameters of the configuration and connection.
     * @return a list of {@link ParameterModel} that will not repeat their {@link ParameterModel#getName()}s.
     */
    private List<ParameterModel> getAllProperties(ExtensionModel extensionModel) {
      List<ParameterModel> result = new ArrayList<>();
      extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME).ifPresent(configurationModel -> {
        result.addAll(configurationModel.getAllParameterModels());
        configurationModel.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME)
            .ifPresent(
                       connectionProviderModel -> result.addAll(connectionProviderModel.getAllParameterModels()));
      });
      return result;
    }

    /**
     * To properly feed the {@link ExpressionManager#evaluate(String, DataType, BindingContext, CoreEvent)} we need to store the
     * {@link MetadataType} per parameter, so that the {@link DataType} can be generated.
     *
     * @param parameters list of parameters taken from the XML
     * @param parameterModels collection of elements taken from the matching {@link ExtensionModel}
     * @return a collection of parameters to be later consumed in {@link #getEvaluatedValue(CoreEvent, String, MetadataType)}
     */
    private Map<String, Pair<String, MetadataType>> parseParameters(Map<String, String> parameters,
                                                                    List<ParameterModel> parameterModels) {
      final Map<String, Pair<String, MetadataType>> result = new HashMap<>();

      for (ParameterModel parameterModel : parameterModels) {
        final String parameterName = parameterModel.getName();
        if (parameters.containsKey(parameterName)) {
          final String xmlValue = parameters.get(parameterName).trim();
          result.put(parameterName, new Pair<>(xmlValue, parameterModel.getType()));
        }
      }
      return result;
    }

    /**
     * Given an {@code event}, it will consume from it ONLY the defined properties and parameters that were set when initializing
     * this class to provide scoping for the inner list of processors.
     */
    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .doOnNext(event -> {
            final DefaultFlowCallStack flowCallStack = (DefaultFlowCallStack) event.getFlowCallStack();

            final ComponentIdentifier identifier = (ComponentIdentifier) getAnnotation(ANNOTATION_NAME);
            if (identifier.getNamespace() == null || "tns".equals(identifier.getNamespace())) {
              final String[] peekedWithNamespace = flowCallStack.peek().getFlowName().split("\\:");
              String peekedNamespace = peekedWithNamespace[0];

              flowCallStack.push(new FlowStackElement(peekedNamespace + ":" + identifier.getName(), null));
            } else {
              flowCallStack.push(new FlowStackElement(identifier.getNamespace() + ":" + identifier.getName(), null));
            }
          })
          .concatMap(request -> {
            Publisher<CoreEvent> child = processWithChildContextDontComplete(createEventWithParameters(request), super::apply,
                                                                             ofNullable(getLocation()));
            return from(child).doOnNext(event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop())
                .onErrorMap(MessagingException.class, remapMessagingException())
                .doOnError(MessagingException.class, me -> ((DefaultFlowCallStack) me.getEvent().getFlowCallStack()).pop())
                .onErrorResume(MessagingException.class, createErrorResumeMapper(request))
                .map(eventResult -> processResult(request, eventResult));
          });
    }

    /**
     * If any of the internals of an <operation/> throws an {@link MuleException}, this method will be responsible of
     * altering the current location of that exception will change to target the call invocation of the smart connector's
     * operation.
     * <br/>
     * By doing so, later processing (such as {@link MuleException#getDetailedMessage()}) will keep digging for the prime
     * cause of the exception, which means the Mule application will <b>only see</b> the logs of the application's call to the
     * smart connector's <operation/>, rather than the internals of the smart connector's internals.
     */
    private Function<MessagingException, Throwable> remapMessagingException() {
      return me -> {
        EnrichedNotificationInfo notificationInfo = createInfo(me.getEvent(), me, null);
        muleContext.getExceptionContextProviders()
            .forEach(cp -> cp.getContextInfo(notificationInfo, this).forEach(me::addInfo));
        return me;
      };
    }

    /**
     * If an exception within the <module/> is thrown, we will cut the current exception bubbling by returning the control to the
     * caller/publisher of the current <module/>'s invocation.
     */
    private Function<MessagingException, Publisher<? extends CoreEvent>> createErrorResumeMapper(CoreEvent originalRequest) {
      return throwable -> {
        throwable = handleSubChainException(throwable, originalRequest);
        return Mono.from(((BaseEventContext) originalRequest.getContext()).error(throwable)).then(Mono.error(throwable));
      };
    }

    /**
     * Unlike other {@link MessageProcessorChain MessageProcessorChains}, modules could contain error mappings that need to be
     * considered when resolving exceptions.
     */
    private MessagingException handleSubChainException(MessagingException messagingException, CoreEvent originalRequest) {
      final CoreEvent.Builder builder = CoreEvent.builder(originalRequest).error(messagingException.getEvent().getError().get());
      List<ErrorMapping> errorMappings = getErrorMappings(this);
      if (!errorMappings.isEmpty()) {
        Error error = messagingException.getEvent().getError().get();
        ErrorType errorType = error.getErrorType();
        ErrorType resolvedType = errorMappings.stream()
            .filter(m -> m.match(errorType))
            .findFirst()
            .map(ErrorMapping::getTarget)
            .orElse(errorType);
        if (!resolvedType.equals(errorType)) {
          builder.error(ErrorBuilder.builder(error).errorType(resolvedType).build());
        }
      }
      messagingException.setProcessedEvent(builder.build());
      return messagingException;
    }

    private CoreEvent processResult(CoreEvent originalEvent, CoreEvent chainEvent) {
      if (!returnsVoid) {
        originalEvent = createNewEventFromJustMessage(originalEvent, chainEvent);
      }
      return originalEvent;
    }

    private CoreEvent createNewEventFromJustMessage(CoreEvent request, CoreEvent response) {
      final CoreEvent.Builder builder = CoreEvent.builder(request);
      if (target.isPresent()) {
        TypedValue result =
            expressionManager.evaluate(targetValue, getTargetBindingContext(response.getMessage()));
        builder.addVariable(target.get(), result.getValue(), result.getDataType());
      } else {
        builder.message(builder(response.getMessage()).build());
      }
      return builder.build();
    }

    private CoreEvent createEventWithParameters(CoreEvent event) {
      CoreEvent.Builder builder = CoreEvent.builder(event.getContext());
      builder.message(builder().nullValue().build());
      addVariables(event, builder, properties);
      addVariables(event, builder, parameters);
      return builder.build();
    }

    private void addVariables(CoreEvent event, CoreEvent.Builder builder,
                              Map<String, Pair<String, MetadataType>> unevaluatedMap) {
      unevaluatedMap.entrySet().stream()
          .forEach(entry -> {
            final boolean isExpression = expressionManager.isExpression(entry.getValue().getFirst());
            if (isExpression) {
              final TypedValue<?> evaluatedValue =
                  getEvaluatedValue(event, entry.getValue().getFirst(), entry.getValue().getSecond());
              builder.addVariable(entry.getKey(), evaluatedValue.getValue(), evaluatedValue.getDataType());
            } else {
              builder.addVariable(entry.getKey(), entry.getValue().getFirst());
            }
          });
    }

    private TypedValue<?> getEvaluatedValue(CoreEvent event, String value, MetadataType metadataType) {
      ComponentLocation headLocation;
      final Processor head = getProcessorsToExecute().get(0);
      headLocation = ((Component) head).getLocation();

      TypedValue<?> evaluatedResult;
      if (MetadataFormat.JAVA.equals(metadataType.getMetadataFormat())) {
        evaluatedResult = expressionManager.evaluate(value, event, headLocation);
      } else {
        final String mediaType = metadataType.getMetadataFormat().getValidMimeTypes().iterator().next();
        final DataType expectedOutputType =
            DataType.builder()
                .type(String.class)
                .mediaType(mediaType)
                .charset(UTF_8)
                .build();
        evaluatedResult = expressionManager
            .evaluate(value, expectedOutputType, NULL_BINDING_CONTEXT, event, headLocation, false);
      }
      return evaluatedResult;
    }
  }
}
