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
import static org.mule.runtime.core.internal.message.InternalMessage.builder;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.getErrorMappings;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static reactor.core.publisher.Flux.from;

import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.utils.MetadataTypeUtils;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

  private Map<String, String> properties;
  private Map<String, String> parameters;
  private ExtensionModel extensionModel;
  private OperationModel operationModel;
  private ExpressionManager expressionManager;

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
  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorForLifecycle) {
    return new ModuleOperationProcessorChain("wrapping-operation-module-chain", head, processors, processorForLifecycle,
                                             properties, parameters,
                                             extensionModel, operationModel,
                                             expressionManager,
                                             processingStrategy);
  }

  /**
   * Generates message processor for a specific set of parameters & properties to be added in a new event.
   */
  static public class ModuleOperationProcessorChain extends DefaultMessageProcessorChain
      implements Processor {

    private Map<String, Pair<String, MetadataType>> properties;
    private Map<String, Pair<String, MetadataType>> parameters;
    private boolean returnsVoid;
    private ExpressionManager expressionManager;
    private Optional<String> target;
    private String targetValue;

    ModuleOperationProcessorChain(String name, Processor head, List<Processor> processors,
                                  List<Processor> processorsForLifecycle,
                                  Map<String, String> properties, Map<String, String> parameters,
                                  ExtensionModel extensionModel, OperationModel operationModel,
                                  ExpressionManager expressionManager,
                                  ProcessingStrategy processingStrategy) {
      super(name, ofNullable(processingStrategy), head, processors, processorsForLifecycle);
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
     * To properly feed the {@link ExpressionManager#evaluate(String, DataType, BindingContext, CoreEvent)} we need to store
     * the {@link MetadataType} per parameter, so that the {@link DataType} can be generated.
     *
     * @param parameters list of parameters taken from the XML
     * @param parameterModels collection of elements taken from the matching {@link ExtensionModel}
     * @return a collection of parameters to be later consumed in {@link #getEvaluatedValue(CoreEvent, String, MetadataType)}
     */
    private Map<String, Pair<String, MetadataType>> parseParameters(Map<String, String> parameters,
                                                                    List<ParameterModel> parameterModels) {
      final HashMap<String, Pair<String, MetadataType>> result = new HashMap<>();

      for (ParameterModel parameterModel : parameterModels) {
        final String parameterName = parameterModel.getName();
        if (parameters.containsKey(parameterName)) {
          final String xmlValue = parameters.get(parameterName);
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
          .concatMap(request -> from(processWithChildContext(createEventWithParameters(request), super::apply,
                                                             ofNullable(getLocation())))
                                                                 .onErrorResume(MessagingException.class,
                                                                                createErrorResumeMapper(request))
                                                                 .map(eventResult -> processResult(request, eventResult)));
    }

    /**
     * If an exception within the <module/> is thrown, we will cut the current exception bubbling by returning the control to the
     * caller/publisher of the current <module/>'s invocation.
     */
    private Function<MessagingException, Publisher<? extends CoreEvent>> createErrorResumeMapper(
                                                                                                 CoreEvent originalRequest) {
      return throwable -> {
        throwable = handleSubChainException(throwable, originalRequest);
        return Mono.from(((BaseEventContext) originalRequest.getContext()).error(throwable)).then(Mono.empty());
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
            final Object value = expressionManager.isExpression(entry.getValue().getFirst())
                ? getEvaluatedValue(event, entry.getValue().getFirst(), entry.getValue().getSecond())
                : entry.getValue().getFirst();

            builder.addVariable(entry.getKey(), value);

          });
    }

    private Object getEvaluatedValue(CoreEvent event, String value, MetadataType metadataType) {
      ComponentLocation headLocation;
      final Processor head = getProcessorsToExecute().get(0);
      headLocation = ((Component) head).getLocation();

      Object evaluatedResult;
      if (MetadataFormat.JAVA.equals(metadataType.getMetadataFormat())) {
        evaluatedResult = expressionManager.evaluate(value, event, headLocation).getValue();
      } else {
        final String mediaType = metadataType.getMetadataFormat().getValidMimeTypes().iterator().next();
        final DataType expectedOutputType =
            DataType.builder()
                .type(String.class)
                .mediaType(mediaType)
                .charset(UTF_8)
                .build();
        evaluatedResult = expressionManager
            .evaluate(value, expectedOutputType, NULL_BINDING_CONTEXT, event, headLocation, false).getValue();
      }
      return evaluatedResult;
    }
  }
}
