/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.runtime.ExecutionTypeMapper.asProcessingType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.fromCallable;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
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
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.OperationExecutionFunction;
import org.mule.runtime.core.policy.OperationPolicy;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultLocationPart;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.metadata.EntityMetadataMediator;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.Exceptions;
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
    implements Processor, EntityMetadataProvider, Lifecycle {

  private static final Logger LOGGER = getLogger(OperationMessageProcessor.class);
  static final String INVALID_TARGET_MESSAGE =
      "Flow '%s' defines an invalid usage of operation '%s' which uses %s as target";

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final ResolverSet resolverSet;
  private final String target;
  private final EntityMetadataMediator entityMetadataMediator;

  private ExecutionMediator executionMediator;
  private OperationExecutor operationExecutor;
  private PolicyManager policyManager;
  protected ReturnDelegate returnDelegate;

  public OperationMessageProcessor(ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, operationModel, configurationProvider, cursorProviderFactory, extensionManager);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.entityMetadataMediator = new EntityMetadataMediator(operationModel);
    this.policyManager = policyManager;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    if (operationModel.isBlocking()) {
      return from(publisher).map(checkedFunction(event -> withContextClassLoader(classLoader, () -> {
        Optional<ConfigurationInstance> configuration = getConfiguration(event);
        Map<String, Object> operationParameters = resolverSet.resolve(event).asMap();

        OperationExecutionFunction operationExecutionFunction = (parameters, operationEvent) -> {
          ExecutionContextAdapter<OperationModel> operationContext =
              createExecutionContext(configuration, parameters, operationEvent);
          return doProcess(operationEvent, operationContext).block();
        };

        OperationPolicy policy =
            policyManager.createOperationPolicy(getLocation(), event,
                                                operationParameters,
                                                operationExecutionFunction);
        return policy.process(event);
      }, MuleException.class, e -> {
        throw new DefaultMuleException(e);
      })));
    }

    return from(publisher).flatMap(checkedFunction(event -> withContextClassLoader(classLoader, () -> {
      Optional<ConfigurationInstance> configuration = getConfiguration(event);
      Map<String, Object> operationParameters = resolverSet.resolve(event).asMap();
      ExecutionContextAdapter<OperationModel> operationContext =
          createExecutionContext(configuration, operationParameters, event);

      // While a hook in reactor is used to map Throwable to MessagingException when an error occurs this does not cover the case
      // where an error is explicitly triggered via a Sink such as such as when using Mono.create in ReactorCompletionCallback
      // rather than being thrown by a reactor operator. Although changes could be made to Mule to cater for this in
      // AbstractMessageProcessorChain, this is not trivial given processor interceptors and a potent performance overhead
      // associated with the addition of many additional flatMaps. It would be slightly clearer to create the
      // MessagingException in ReactorCompletionCallback where Mono.error is used but we don't have a reference to the
      // processor there.
      return doProcess(event, operationContext).onErrorMap(e -> !(e instanceof MessagingException),
                                                           e -> new MessagingException(event, e, this));
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    })));
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
                                         muleContext);
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
        : new TargetReturnDelegate(target, operationModel, getCursorProviderFactory(), muleContext);
  }

  private boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (target.startsWith(FLOW_VARS)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, flowConstruct.getName(), operationModel.getName(),
                                                 format("the '%s' prefix", FLOW_VARS)));
    } else if (muleContext.getExpressionManager().isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, flowConstruct.getName(), operationModel.getName(),
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
                                                 "Flow '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                     + "The selected config does not support that operation.",
                                                 flowConstruct.getName(), operationModel.getName(),
                                                 configurationProvider.getName()));
    }
  }

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    final Event event = getInitialiserEvent(muleContext);
    return new OperationParameterValueResolver(new LazyExecutionContext<>(resolverSet, operationModel, extensionModel, event));
  }

  @Override
  public ProcessingType getProcessingType() {
    return asProcessingType(operationModel.getExecutionType());
  }

  @Override
  public ComponentLocation getLocation() {
    ComponentLocation location = super.getLocation();
    DefaultLocationPart part = new DefaultLocationPart("part", Optional.of(new TypedComponentIdentifier() {

      @Override
      public ComponentType getType() {
        return ComponentType.OPERATION;
      }

      @Override
      public ComponentIdentifier getIdentifier() {
        return ComponentIdentifier.builder().withName("dummy").withNamespace("suads").build();
      }
    }), empty(), empty());
    return location != null ? location : new DefaultComponentLocation(empty(), singletonList(part));
  }
}
