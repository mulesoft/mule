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
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.fromCallable;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

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
 * through which a {@link ComponentExecutorFactory} is obtained. Models with no such property cannot be used with this class. The
 * obtained {@link ComponentExecutor} serve all invocations of {@link #process(InternalEvent)} on {@code this} instance but will
 * not be shared with other instances of {@link ConstructMessageProcessor}. All the {@link Lifecycle} events that {@code this}
 * instance receives will be propagated to the {@link #operationExecutor}.
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
public class ConstructMessageProcessor extends ExtensionComponent<ConstructModel>
    implements Processor, ParametersResolverProcessor<ConstructModel> {

  private static final Logger LOGGER = getLogger(ConstructMessageProcessor.class);
  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as %s";

  protected final ExtensionModel extensionModel;
  protected final ConstructModel constructModel;
  private final ResolverSet resolverSet;
  private final String target;
  private final String targetValue;
  private final RetryPolicyTemplate retryPolicyTemplate;

  private ExecutionMediator executionMediator;
  private ComponentExecutor<ConstructModel> operationExecutor;
  private PolicyManager policyManager;
  protected ReturnDelegate returnDelegate;
  protected final ExtensionManager extensionManager;
  protected final ClassLoader classLoader;
  private CursorProviderFactory cursorProviderFactory;

  public ConstructMessageProcessor(ExtensionModel extensionModel,
                                   ConstructModel constructModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, constructModel, configurationProvider, cursorProviderFactory, extensionManager);

    this.classLoader = getClassLoader(extensionModel);
    this.extensionManager = extensionManager;
    this.extensionModel = extensionModel;
    this.constructModel = constructModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.targetValue = targetValue;
    this.policyManager = policyManager;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.cursorProviderFactory = cursorProviderFactory;
  }

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
    return from(publisher).flatMap(checkedFunction(event -> withContextClassLoader(classLoader, () -> {
      OperationExecutionFunction operationExecutionFunction;

      if (event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
        // If the event already contains an execution context, use that one.
        ExecutionContextAdapter<ConstructModel> operationContext = getPrecalculatedContext(event);

        operationExecutionFunction = (parameters, operationEvent) -> doProcess(operationContext);
      } else {
        operationExecutionFunction = (parameters, operationEvent) -> {
          ExecutionContextAdapter<ConstructModel> operationContext;
          try {
            operationContext = createExecutionContext(parameters, operationEvent);
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
          return doProcess(operationContext)
              .onErrorMap(e -> !(e instanceof MessagingException), e -> new MessagingException(event, e, this));
        };
      }
      if (getLocation() != null) {
        return policyManager
            .createOperationPolicy(this, event, getResolutionResult(event), operationExecutionFunction)
            .process(event);
      } else {
        // If this operation has no component location then it is internal. Don't apply policies on internal operations.
        return operationExecutionFunction.execute(getResolutionResult(event), event);
      }
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    })));
  }

  private PrecalculatedExecutionContextAdapter<ConstructModel> getPrecalculatedContext(InternalEvent event) {
    return (PrecalculatedExecutionContextAdapter) (event.getInternalParameters().get(INTERCEPTION_RESOLVED_CONTEXT));
  }

  protected Mono<InternalEvent> doProcess(ExecutionContextAdapter<ConstructModel> operationContext) {
    return executeOperation(operationContext)
        .map(value -> asReturnValue(operationContext, value))
        .switchIfEmpty(fromCallable(() -> asReturnValue(operationContext, null)))
        .onErrorMap(Exceptions::unwrap);
  }

  private InternalEvent asReturnValue(ExecutionContextAdapter<ConstructModel> operationContext, Object value) {
    if (value instanceof InternalEvent) {
      return (InternalEvent) value;
    } else {
      return returnDelegate.asReturnValue(value, operationContext);
    }
  }

  private Mono<Object> executeOperation(ExecutionContextAdapter operationContext) {
    return Mono.from(executionMediator.execute(operationExecutor, operationContext));
  }

  private ExecutionContextAdapter<ConstructModel> createExecutionContext(Map<String, Object> resolvedParameters,
                                                                         InternalEvent event)
      throws MuleException {

    return new DefaultExecutionContext<>(extensionModel, empty(), resolvedParameters, constructModel, event,
                                         cursorProviderFactory, streamingManager, getLocation(), retryPolicyTemplate,
                                         muleContext);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    returnDelegate = createReturnDelegate();
    operationExecutor = getOperationExecutorFactory(constructModel).createExecutor(constructModel);
    executionMediator = createExecutionMediator();
    initialiseIfNeeded(resolverSet, muleContext);
    initialiseIfNeeded(operationExecutor, true, muleContext);
  }

  private ReturnDelegate createReturnDelegate() {
    return !isTargetPresent()
        ? new ValueReturnDelegate(constructModel, cursorProviderFactory, muleContext)
        : new TargetReturnDelegate(target, targetValue, constructModel, cursorProviderFactory, muleContext);
  }

  private boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (muleContext.getExpressionManager().isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 constructModel.getName(),
                                                 "an expression", TARGET_PARAMETER_NAME));
    } else if (!muleContext.getExpressionManager().isExpression(targetValue)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 constructModel.getName(), "something that is not an expression",
                                                 TARGET_VALUE_PARAMETER_NAME));
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

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel, constructModel, connectionManager, muleContext.getErrorTypeRepository());
  }

  @Override
  public ProcessingType getProcessingType() {
    return CPU_LITE_ASYNC;
  }

  @Override
  public void resolveParameters(InternalEvent.Builder eventBuilder,
                                BiConsumer<Map<String, Object>, ExecutionContext> afterConfigurer)
      throws MuleException {
    if (operationExecutor instanceof OperationArgumentResolverFactory) {
      PrecalculatedExecutionContextAdapter executionContext =
          new PrecalculatedExecutionContextAdapter<>(createExecutionContext(eventBuilder.build()), operationExecutor);

      final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

      List<Interceptor> interceptors =
          mediator.collectInterceptors(executionContext.getConfiguration(), executionContext.getOperationExecutor());
      InterceptorsExecutionResult beforeExecutionResult = mediator.before(executionContext, interceptors);

      if (beforeExecutionResult.isOk()) {
        final Map<String, Object> resolvedArguments = ((OperationArgumentResolverFactory<ConstructModel>) operationExecutor)
            .createArgumentResolver(null).apply(executionContext);

        afterConfigurer.accept(resolvedArguments, executionContext);
        executionContext.changeEvent(eventBuilder.build());
      } else {
        disposeResolvedParameters(executionContext, interceptors);
        throw new DefaultMuleException("Interception execution for operation not ok", beforeExecutionResult.getThrowable());
      }
    }
  }

  @Override
  public void disposeResolvedParameters(ExecutionContext<ConstructModel> executionContext) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;
    List<Interceptor> interceptors = mediator.collectInterceptors(executionContext.getConfiguration(),
                                                                  executionContext instanceof PrecalculatedExecutionContextAdapter
                                                                      ? ((PrecalculatedExecutionContextAdapter) executionContext)
                                                                          .getOperationExecutor()
                                                                      : operationExecutor);

    disposeResolvedParameters(executionContext, interceptors);
  }

  private void disposeResolvedParameters(ExecutionContext<ConstructModel> executionContext, List<Interceptor> interceptors) {
    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;

    mediator.after(executionContext, null, interceptors);
  }

  private ExecutionContextAdapter<ConstructModel> createExecutionContext(InternalEvent event) throws MuleException {
    return createExecutionContext(getResolutionResult(event), event);
  }

  private Map<String, Object> getResolutionResult(InternalEvent event)
      throws MuleException {
    return resolverSet.resolve(from(event, empty())).asMap();
  }


  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {

  }

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    final InternalEvent event = getInitialiserEvent(muleContext);
    return new OperationParameterValueResolver<>(new LazyExecutionContext<>(resolverSet, constructModel, extensionModel,
                                                                            from(event)));
  }

}
