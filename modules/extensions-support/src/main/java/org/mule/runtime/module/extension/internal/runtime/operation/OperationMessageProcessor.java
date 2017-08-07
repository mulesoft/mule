/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.module.extension.internal.runtime.ExecutionTypeMapper.asProcessingType;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.fromCallable;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.EntityMetadataProvider;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.metadata.EntityMetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

/**
 * A {@link Processor} capable of executing extension operations.
 * <p>
 * It obtains a configuration instance, evaluate all the operation parameters and executes a {@link OperationModel} by using a
 * {@link #operationExecutor}. This message processor is capable of serving the execution of any {@link OperationModel} of any
 * {@link ExtensionModel}.
 * <p>
 * A {@link #operationExecutor} is obtained by testing the {@link OperationModel} for a {@link OperationExecutorModelProperty}
 * through which a {@link OperationExecutorFactory} is obtained. Models with no such property cannot be used with this class. The
 * obtained {@link OperationExecutor} serve all invocations of {@link #process(Event)} on {@code this} instance but will not be
 * shared with other instances of {@link OperationMessageProcessor}. All the {@link Lifecycle} events that {@code this} instance
 * receives will be propagated to the {@link #operationExecutor}.
 * <p>
 * The {@link #operationExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 * <p>
 * Before executing the operation will use the {@link PolicyManager} to lookup for a {@link OperationPolicy} that must be applied
 * to the operation. If there's a policy to be applied then it will interleave the operation execution with the policy logic
 * allowing the policy to execute logic over the operation parameters, change those parameters and then execute logic with the
 * operation response.
 *
 * @since 3.7.0
 */
public class OperationMessageProcessor extends ExtensionComponent<OperationModel>
    implements Processor, ParametersResolverProcessor, EntityMetadataProvider, Lifecycle {

  private static final Logger LOGGER = getLogger(OperationMessageProcessor.class);
  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as target";

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final ResolverSet resolverSet;
  private final String target;
  private final TargetType targetType;
  private final EntityMetadataMediator entityMetadataMediator;

  private ExecutionMediator executionMediator;
  private OperationExecutor operationExecutor;
  private PolicyManager policyManager;
  protected ReturnDelegate returnDelegate;

  public OperationMessageProcessor(ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   TargetType targetType,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, operationModel, configurationProvider, cursorProviderFactory, extensionManager);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.targetType = targetType;
    this.entityMetadataMediator = new EntityMetadataMediator(operationModel);
    this.policyManager = policyManager;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).flatMap(checkedFunction(event -> withContextClassLoader(classLoader, () -> {
      Optional<ConfigurationInstance> configuration;
      OperationExecutionFunction operationExecutionFunction;

      if (event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
        // If the event already contains an execution context, use that one.
        ExecutionContextAdapter<OperationModel> operationContext = getPrecalculatedContext(event);
        configuration = operationContext.getConfiguration();

        operationExecutionFunction = (parameters, operationEvent) -> doProcess(operationEvent, operationContext);
      } else {
        // Otherwise, generate the context as usual.
        configuration = getConfiguration(event);

        operationExecutionFunction = (parameters, operationEvent) -> {
          ExecutionContextAdapter<OperationModel> operationContext;
          try {
            operationContext = createExecutionContext(configuration, parameters, operationEvent);
          } catch (MuleException e) {
            return error(e);
          }
          // While a hook in reactor is used to map Throwable to MessagingException when an error occurs this does not cover
          // the case where an error is explicitly triggered via a Sink such as such as when using Mono.create in
          // ReactorCompletionCallback rather than being thrown by a reactor operator. Although changes could be made to Mule
          // to cater for this in AbstractMessageProcessorChain, this is not trivial given processor interceptors and a potent
          // performance overhead associated with the addition of many additional flatMaps. It would be slightly clearer to
          // create the MessagingException in ReactorCompletionCallback where Mono.error is used but we don't have a reference
          // to the processor there.
          return doProcess(operationEvent, operationContext)
              .onErrorMap(e -> !(e instanceof MessagingException),
                          e -> {
                            // MULE-13009 Inconsistent error propagation in extension operations depending on operation type
                            if (operationModel.isBlocking()) {
                              return new MessagingException(event, e);
                            } else {
                              return new MessagingException(event, e, this);
                            }
                          });
        };
      }
      if (getLocation() != null) {
        return policyManager
            .createOperationPolicy(getLocation(), event, getResolutionResult(event, configuration), operationExecutionFunction)
            .process(event);
      } else {
        // If this operation has no component location then it is internal. Don't apply policies on internal operations.
        return operationExecutionFunction.execute(getResolutionResult(event, configuration), event);
      }
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    })));
  }

  private PrecalculatedExecutionContextAdapter getPrecalculatedContext(Event event) {
    return (PrecalculatedExecutionContextAdapter) (event.getInternalParameters().get(INTERCEPTION_RESOLVED_CONTEXT));
  }

  protected Mono<Event> doProcess(Event event, ExecutionContextAdapter<OperationModel> operationContext) {
    return executeOperation(operationContext)
        .map(value -> returnDelegate.asReturnValue(value, operationContext))
        .switchIfEmpty(fromCallable(() -> returnDelegate.asReturnValue(null, operationContext)))
        .onErrorMap(Exceptions::unwrap);
  }

  private Mono<Object> executeOperation(ExecutionContextAdapter operationContext) {
    return Mono.from(executionMediator.execute(operationExecutor, operationContext));
  }

  private ExecutionContextAdapter<OperationModel> createExecutionContext(Optional<ConfigurationInstance> configuration,
                                                                         Map<String, Object> resolvedParameters,
                                                                         Event event)
      throws MuleException {

    return new DefaultExecutionContext<>(extensionModel, configuration, resolvedParameters, operationModel, event,
                                         getCursorProviderFactory(), streamingManager, getLocation(), muleContext);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    returnDelegate = createReturnDelegate();
    operationExecutor = getOperationExecutorFactory(operationModel).createExecutor(operationModel);
    executionMediator = createExecutionMediator();
    initialiseIfNeeded(resolverSet, muleContext);
    initialiseIfNeeded(operationExecutor, true, muleContext);
  }

  private ReturnDelegate createReturnDelegate() {
    if (isVoid(operationModel)) {
      return VoidReturnDelegate.INSTANCE;
    }

    return !isTargetPresent()
        ? new ValueReturnDelegate(operationModel, getCursorProviderFactory(), muleContext)
        : new TargetReturnDelegate(target, targetType, operationModel, getCursorProviderFactory(), muleContext);
  }

  private boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (target.startsWith(FLOW_VARS)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 operationModel.getName(),
                                                 format("the '%s' prefix", FLOW_VARS)));
    } else if (muleContext.getExpressionManager().isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 operationModel.getName(),
                                                 "an expression"));
    }

    return true;
  }

  protected Optional<String> getTarget() {
    return isTargetPresent() ? of(target) : empty();
  }

  @Override
  public void doStart() throws MuleException {
    startIfNeeded(operationExecutor);
  }

  @Override
  public void doStop() throws MuleException {
    stopIfNeeded(operationExecutor);
  }

  @Override
  public void doDispose() {
    disposeIfNeeded(operationExecutor, LOGGER);
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys() throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(getClassLoader(this.extensionModel),
                                                                      () -> entityMetadataMediator.getEntityKeys(context)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(MetadataKey key) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(context -> withContextClassLoader(classLoader, () -> entityMetadataMediator
          .getEntityMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel, operationModel, connectionManager, muleContext.getErrorTypeRepository());
  }

  /**
   * Validates that the {@link #operationModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalOperationException If the validation fails
   */
  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    ConfigurationModel configurationModel = configurationProvider.getConfigurationModel();
    if (!configurationModel.getOperationModel(operationModel.getName()).isPresent() &&
        !configurationProvider.getExtensionModel().getOperationModel(operationModel.getName()).isPresent()) {
      throw new IllegalOperationException(format(
                                                 "Root component '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                     + "The selected config does not support that operation.",
                                                 getLocation().getRootContainerName(), operationModel.getName(),
                                                 configurationProvider.getName()));
    }
  }

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    final Event event = getInitialiserEvent(muleContext);
    return new OperationParameterValueResolver(new LazyExecutionContext<>(resolverSet, operationModel, extensionModel,
                                                                          from(event)));
  }

  @Override
  public ProcessingType getProcessingType() {
    ProcessingType processingType = asProcessingType(operationModel.getExecutionType());
    if (processingType == CPU_LITE && !operationModel.isBlocking()) {
      // If processing type is CPU_LITE and operation is non-blocking then use CPU_LITE_ASYNC processing type so that the Flow can
      // return processing to a Flow thread.
      return CPU_LITE_ASYNC;
    } else {
      return processingType;
    }
  }

  @Override
  public ParametersResolverProcessorResult resolveParameters(Event event) throws MuleException {
    if (operationExecutor instanceof OperationArgumentResolverFactory) {
      PrecalculatedExecutionContextAdapter executionContext =
          new PrecalculatedExecutionContextAdapter(createExecutionContext(event), operationExecutor);

      final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

      List<Interceptor> interceptors =
          mediator.collectInterceptors(executionContext.getConfiguration(), executionContext.getOperationExecutor());
      InterceptorsExecutionResult beforeExecutionResult = mediator.before(executionContext, interceptors);

      if (beforeExecutionResult.isOk()) {
        final Map<String, Object> resolvedArguments = ((OperationArgumentResolverFactory) operationExecutor)
            .createArgumentResolver(operationModel).apply(executionContext);

        return new ParametersResolverProcessorResult(resolvedArguments, executionContext);
      } else {
        disposeResolvedParameters(executionContext, interceptors);
        throw new DefaultMuleException("Interception execution for operation not ok", beforeExecutionResult.getThrowable());
      }
    } else {
      return new ParametersResolverProcessorResult(emptyMap(), null);
    }
  }

  @Override
  public void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;
    List<Interceptor> interceptors = mediator.collectInterceptors(executionContext.getConfiguration(),
                                                                  executionContext instanceof PrecalculatedExecutionContextAdapter
                                                                      ? ((PrecalculatedExecutionContextAdapter) executionContext)
                                                                          .getOperationExecutor()
                                                                      : operationExecutor);

    disposeResolvedParameters(executionContext, interceptors);
  }

  private void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext, List<Interceptor> interceptors) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

    mediator.after(executionContext, null, interceptors);
  }

  private ExecutionContextAdapter<OperationModel> createExecutionContext(Event event) throws MuleException {
    Optional<ConfigurationInstance> configuration = getConfiguration(event);
    return createExecutionContext(configuration, getResolutionResult(event, configuration), event);
  }

  private Map<String, Object> getResolutionResult(Event event, Optional<ConfigurationInstance> configuration)
      throws MuleException {
    return resolverSet.resolve(from(event, configuration)).asMap();
  }
}
