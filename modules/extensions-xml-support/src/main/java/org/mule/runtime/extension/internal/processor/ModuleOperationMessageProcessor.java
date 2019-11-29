/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.internal.message.InternalMessage.builder;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.getErrorMappings;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
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
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
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
import org.mule.runtime.core.api.exception.BaseExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Creates a chain for any operation, where it parameterizes two type of values (parameter and property) to the inner processors
 * through the {@link CoreEvent}.
 * <p>
 * Both parameter and property could be simple literals or expressions that will be evaluated before passing the new
 * {@link CoreEvent} to the child processors.
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
public class ModuleOperationMessageProcessor extends AbstractMessageProcessorOwner implements Processor {

  private static final String ORIGINAL_EVENT_KEY = "mule.xmlSdk.originalEvent";

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  private MessageProcessorChain nestedChain;
  private List<Processor> processors;

  private final Map<String, Pair<String, MetadataType>> properties;
  private final Map<String, Pair<String, MetadataType>> parameters;
  private final boolean returnsVoid;
  private final Optional<String> target;
  private final String targetValue;

  public ModuleOperationMessageProcessor(Map<String, String> properties,
                                         Map<String, String> parameters,
                                         ExtensionModel extensionModel, OperationModel operationModel) {
    this.properties = parseParameters(properties, getAllProperties(extensionModel));
    this.parameters = parseParameters(parameters, operationModel.getAllParameterModels());
    this.returnsVoid = MetadataTypeUtils.isVoid(operationModel.getOutput().getType());
    this.target = parameters.containsKey(TARGET_PARAMETER_NAME) ? of(parameters.remove(TARGET_PARAMETER_NAME)) : empty();
    this.targetValue = parameters.remove(TARGET_VALUE_PARAMETER_NAME);
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
          .ifPresent(connectionProviderModel -> result.addAll(connectionProviderModel.getAllParameterModels()));
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

  @Override
  public CoreEvent process(final CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  /**
   * Given an {@code event}, it will consume from it ONLY the defined properties and parameters that were set when initializing
   * this class to provide scoping for the inner list of processors.
   */
  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(this::pushFlowStackEntry)
        .transform(eventPub -> applyWithChildContext(from(eventPub).map(this::createEventWithParameters),
                                                     nestedChain, ofNullable(getLocation()), errorHandler()))
        .doOnNext(event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop())
        .map(eventResult -> processResult(getInternalParameter(ORIGINAL_EVENT_KEY, eventResult), eventResult));
  }

  private void pushFlowStackEntry(CoreEvent event) {
    final DefaultFlowCallStack flowCallStack = (DefaultFlowCallStack) event.getFlowCallStack();

    flowCallStack.push(createFlowStackEntry(flowCallStack.peek()));
  }

  private FlowStackElement createFlowStackEntry(FlowStackElement top) {
    final ComponentIdentifier identifier = (ComponentIdentifier) getAnnotation(ANNOTATION_NAME);
    if (identifier.getNamespace() == null || "tns".equals(identifier.getNamespace())) {
      final String[] peekedWithNamespace = top.getFlowName().split("\\:");
      String peekedNamespace = peekedWithNamespace[0];

      return new FlowStackElement(peekedNamespace + ":" + identifier.getName(), null);
    } else {
      return new FlowStackElement(identifier.getNamespace() + ":" + identifier.getName(), null);
    }
  }

  private BaseExceptionHandler errorHandler() {
    return new BaseExceptionHandler() {

      @Override
      protected void onError(Exception exception) {
        final MessagingException me = (MessagingException) exception;
        final CoreEvent event = me.getEvent();

        // If any of the internals of an <operation/> throws an {@link MuleException}, this method will be responsible of altering
        // the current location of that exception will change to target the call invocation of the smart connector's operation.
        // <p>
        // By doing so, later processing (such as {@link MuleException#getDetailedMessage()}) will keep digging for the prime
        // cause of the exception, which means the Mule application will <b>only see</b> the logs of the application's call to the
        // smart connector's <operation/>, rather than the internals of the smart connector's internals.
        EnrichedNotificationInfo notificationInfo = createInfo(event, me, null);
        exceptionContextProviders
            .forEach(cp -> cp.getContextInfo(notificationInfo, ModuleOperationMessageProcessor.this)
                .forEach(me::addInfo));

        ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
        handleSubChainException(me,
                                getInternalParameter(ORIGINAL_EVENT_KEY, event,
                                                     ((BaseEventContext) event.getContext()).getParentContext().get()));
      }

      /**
       * Unlike other {@link MessageProcessorChain MessageProcessorChains}, modules could contain error mappings that need to be
       * considered when resolving exceptions.
       */
      private void handleSubChainException(MessagingException messagingException, CoreEvent originalRequest) {
        final CoreEvent.Builder builder = CoreEvent.builder(messagingException.getEvent().getContext(), originalRequest)
            .error(messagingException.getEvent().getError().get());
        List<ErrorMapping> errorMappings = getErrorMappings(ModuleOperationMessageProcessor.this);
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
      }
    };
  }

  private String getParameterId(String keyPrefix, CoreEvent event) {
    return getParameterId(keyPrefix, event.getContext());
  }

  private String getParameterId(String keyPrefix, final EventContext context) {
    return keyPrefix + context.getId();
  }

  private <T> T getInternalParameter(String keyPrefix, CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(getParameterId(keyPrefix, event));
  }

  private <T> T getInternalParameter(String keyPrefix, CoreEvent event, EventContext context) {
    return ((InternalEvent) event).getInternalParameter(getParameterId(keyPrefix, context));
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
    InternalEvent.Builder builder = InternalEvent.builder(event.getContext());
    builder.message(builder().nullValue().build());
    addVariables(event, builder, properties);
    addVariables(event, builder, parameters);
    builder.internalParameters(((InternalEvent) event).getInternalParameters());
    builder.addInternalParameter(getParameterId(ORIGINAL_EVENT_KEY, event), event);
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
    final Processor head = nestedChain.getMessageProcessors().get(0);
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

  /**
   * Configure the nested {@link Processor}'s that error handling and transactional behaviour should be applied to.
   *
   * @param processors
   */
  public void setMessageProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.nestedChain = buildNewChainWithListOfProcessors(getProcessingStrategy(locator, getRootContainerLocation()), processors);
    super.initialise();
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public void start() throws MuleException {
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    super.stop();
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(nestedChain);
  }
}
