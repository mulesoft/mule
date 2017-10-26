/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.fromCallable;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.processor.ParametersResolverProcessor;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

/**
 * A {@link Processor} capable of executing extension components.
 * <p>
 * If required, it obtains a configuration instance, evaluate all the operation parameters and executes it by using a
 * {@link #componentExecutor}. This message processor is capable of serving the execution of any {@link } of any
 * {@link ExtensionModel}.
 * <p>
 * A {@link #componentExecutor} is obtained by testing the {@link T} for a {@link ComponentExecutorModelProperty}
 * through which a {@link ComponentExecutorFactory} is obtained. Models with no such property cannot be used with this class. The
 * obtained {@link ComponentExecutor} serve all invocations of {@link #process(CoreEvent)} on {@code this} instance but will
 * not be shared with other instances of {@link ComponentMessageProcessor}. All the {@link Lifecycle} events that {@code this}
 * instance receives will be propagated to the {@link #componentExecutor}.
 * <p>
 * The {@link #componentExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 * <p>
 * Before executing the operation will use the {@link PolicyManager} to lookup for a {@link OperationPolicy} that must be applied
 * to the operation. If there's a policy to be applied then it will interleave the operation execution with the policy logic
 * allowing the policy to execute logic over the operation parameters, change those parameters and then execute logic with the
 * operation response.
 *
 * @since 4.0
 */
public abstract class ComponentMessageProcessor<T extends ComponentModel> extends ExtensionComponent<T>
    implements Processor, ParametersResolverProcessor<T>, Lifecycle {

  private static final Logger LOGGER = getLogger(ComponentMessageProcessor.class);
  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as %s";

  protected final ExtensionModel extensionModel;
  protected final ResolverSet resolverSet;
  protected final String target;
  protected final String targetValue;

  protected final RetryPolicyTemplate retryPolicyTemplate;

  protected ExecutionMediator executionMediator;
  protected ComponentExecutor componentExecutor;
  protected PolicyManager policyManager;
  protected ReturnDelegate returnDelegate;

  public ComponentMessageProcessor(ExtensionModel extensionModel,
                                   T componentModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, componentModel, configurationProvider, cursorProviderFactory, extensionManager);
    this.extensionModel = extensionModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.targetValue = targetValue;
    this.policyManager = policyManager;
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher).flatMap(checkedFunction(event -> {
      Optional<ConfigurationInstance> configuration;
      OperationExecutionFunction operationExecutionFunction;

      if (((InternalEvent) event).getInternalParameters().containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
        // If the event already contains an execution context, use that one.
        ExecutionContextAdapter<T> operationContext = getPrecalculatedContext(event);
        configuration = operationContext.getConfiguration();

        operationExecutionFunction = (parameters, operationEvent) -> doProcess(operationEvent, operationContext);
      } else {
        // Otherwise, generate the context as usual.
        configuration = getConfiguration(event);

        operationExecutionFunction = (parameters, operationEvent) -> {
          ExecutionContextAdapter<T> operationContext;
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
              .onErrorMap(e -> !(e instanceof MessagingException), e -> new MessagingException(event, e, this));
        };
      }
      if (getLocation() != null) {
        return policyManager
            .createOperationPolicy(this, event, getResolutionResult(event, configuration), operationExecutionFunction)
            .process(event);
      } else {
        // If this operation has no component location then it is internal. Don't apply policies on internal operations.
        return operationExecutionFunction.execute(getResolutionResult(event, configuration), event);
      }
    }));
  }

  private PrecalculatedExecutionContextAdapter<T> getPrecalculatedContext(CoreEvent event) {
    return (PrecalculatedExecutionContextAdapter) (((InternalEvent) event).getInternalParameters()
        .get(INTERCEPTION_RESOLVED_CONTEXT));
  }

  protected Mono<CoreEvent> doProcess(CoreEvent event, ExecutionContextAdapter<T> operationContext) {
    return executeOperation(operationContext)
        .map(value -> asReturnValue(operationContext, value))
        .switchIfEmpty(fromCallable(() -> asReturnValue(operationContext, null)))
        .onErrorMap(Exceptions::unwrap);
  }

  private CoreEvent asReturnValue(ExecutionContextAdapter<T> operationContext, Object value) {
    if (value instanceof CoreEvent) {
      return (CoreEvent) value;
    } else {
      return returnDelegate.asReturnValue(value, operationContext);
    }
  }

  private Mono<Object> executeOperation(ExecutionContextAdapter operationContext) {
    return Mono.from(executionMediator.execute(componentExecutor, operationContext));
  }

  private ExecutionContextAdapter<T> createExecutionContext(Optional<ConfigurationInstance> configuration,
                                                            Map<String, Object> resolvedParameters,
                                                            CoreEvent event)
      throws MuleException {

    return new DefaultExecutionContext<>(extensionModel, configuration, resolvedParameters, componentModel, event,
                                         getCursorProviderFactory(), streamingManager, getLocation(), retryPolicyTemplate,
                                         muleContext);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    returnDelegate = createReturnDelegate();
    initialiseIfNeeded(resolverSet, muleContext);
    componentExecutor = createComponentExecutor();
    executionMediator = createExecutionMediator();
    initialiseIfNeeded(componentExecutor, true, muleContext);
  }

  private ComponentExecutor<T> createComponentExecutor() {
    Map<String, Object> params = new HashMap<>();
    componentModel.getAllParameterModels().stream()
        .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
        .forEach(p -> {
          ValueResolver<?> resolver = resolverSet.getResolvers().get(p.getName());
          if (resolver != null) {
            try {
              params.put(p.getName(), resolveValue(resolver, ValueResolvingContext.from(getInitialiserEvent())));
            } catch (MuleException e) {
              throw new MuleRuntimeException(e);
            }
          }
        });

    return getOperationExecutorFactory(componentModel).createExecutor(componentModel, params);
  }

  protected ReturnDelegate createReturnDelegate() {
    if (isVoid(componentModel)) {
      return VoidReturnDelegate.INSTANCE;
    }

    return !isTargetPresent()
        ? getValueReturnDelegate()
        : getTargetReturnDelegate();
  }


  protected TargetReturnDelegate getTargetReturnDelegate() {
    return new TargetReturnDelegate(target, targetValue, componentModel, cursorProviderFactory, muleContext);
  }

  protected ValueReturnDelegate getValueReturnDelegate() {
    return new ValueReturnDelegate(componentModel, cursorProviderFactory, muleContext);
  }

  protected boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (muleContext.getExpressionManager().isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(),
                                                 "an expression", TARGET_PARAMETER_NAME));
    } else if (!muleContext.getExpressionManager().isExpression(targetValue)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(), "something that is not an expression",
                                                 TARGET_VALUE_PARAMETER_NAME));
    }

    return true;
  }

  protected Optional<String> getTarget() {
    return isTargetPresent() ? of(target) : empty();
  }

  @Override
  public void doStart() throws MuleException {
    startIfNeeded(componentExecutor);
  }

  @Override
  public void doStop() throws MuleException {
    stopIfNeeded(componentExecutor);
  }

  @Override
  public void doDispose() {
    disposeIfNeeded(componentExecutor, LOGGER);
  }

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel, componentModel, connectionManager, muleContext.getErrorTypeRepository());
  }

  /**
   * Validates that the {@link #componentModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalOperationException If the validation fails
   */
  @Override
  protected abstract void validateOperationConfiguration(ConfigurationProvider configurationProvider);

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    final CoreEvent event = getInitialiserEvent(muleContext);
    return new OperationParameterValueResolver(new LazyExecutionContext<>(resolverSet, componentModel, extensionModel,
                                                                          from(event)));
  }


  @Override
  public abstract ProcessingType getProcessingType();

  @Override
  public void resolveParameters(CoreEvent.Builder eventBuilder,
                                BiConsumer<Map<String, Object>, ExecutionContext> afterConfigurer)
      throws MuleException {
    if (componentExecutor instanceof OperationArgumentResolverFactory) {
      PrecalculatedExecutionContextAdapter executionContext =
          new PrecalculatedExecutionContextAdapter(createExecutionContext(eventBuilder.build()), componentExecutor);

      final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

      List<Interceptor> interceptors =
          mediator.collectInterceptors(executionContext.getConfiguration(), executionContext.getOperationExecutor());
      InterceptorsExecutionResult beforeExecutionResult = mediator.before(executionContext, interceptors);

      if (beforeExecutionResult.isOk()) {
        final Map<String, Object> resolvedArguments = ((OperationArgumentResolverFactory<T>) componentExecutor)
            .createArgumentResolver(componentModel).apply(executionContext);

        afterConfigurer.accept(resolvedArguments, executionContext);
        executionContext.changeEvent(eventBuilder.build());
      } else {
        disposeResolvedParameters(executionContext, interceptors);
        throw new DefaultMuleException("Interception execution for operation not ok", beforeExecutionResult.getThrowable());
      }
    }
  }

  @Override
  public void disposeResolvedParameters(ExecutionContext<T> executionContext) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;
    List<Interceptor> interceptors = mediator.collectInterceptors(executionContext.getConfiguration(),
                                                                  executionContext instanceof PrecalculatedExecutionContextAdapter
                                                                      ? ((PrecalculatedExecutionContextAdapter) executionContext)
                                                                          .getOperationExecutor()
                                                                      : componentExecutor);

    disposeResolvedParameters(executionContext, interceptors);
  }

  private void disposeResolvedParameters(ExecutionContext<T> executionContext, List<Interceptor> interceptors) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

    mediator.after(executionContext, null, interceptors);
  }

  private ExecutionContextAdapter<T> createExecutionContext(CoreEvent event) throws MuleException {
    Optional<ConfigurationInstance> configuration = getConfiguration(event);
    return createExecutionContext(configuration, getResolutionResult(event, configuration), event);
  }

  private Map<String, Object> getResolutionResult(CoreEvent event, Optional<ConfigurationInstance> configuration)
      throws MuleException {
    return resolverSet.resolve(from(event, configuration)).asMap();
  }
}
