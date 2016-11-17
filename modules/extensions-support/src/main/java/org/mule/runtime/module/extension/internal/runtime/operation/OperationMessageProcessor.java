/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.EntityMetadataProvider;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.NextOperation;
import org.mule.runtime.core.policy.Policy;
import org.mule.runtime.core.policy.PolicyProvider;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.metadata.EntityMetadataMediator;
import org.mule.runtime.module.extension.internal.model.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

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
 * Before executing the operation will use the {@link PolicyManager} to lookup for a {@link Policy} that must be applied
 * to the operation. If there's a policy to be applied then it will interleave the operation execution with the policy logic
 * allowing the policy to execute logic over the operation parameters, change those parameters and then execute logic with
 * the operation response.
 *
 * @since 3.7.0
 */
public class OperationMessageProcessor extends ExtensionComponent implements Processor, EntityMetadataProvider, Lifecycle {

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
                                   ExtensionManagerAdapter extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, operationModel, configurationProvider, extensionManager);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.entityMetadataMediator = new EntityMetadataMediator(operationModel);
    this.policyManager = policyManager;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return withContextClassLoader(getExtensionClassLoader(), () -> {
      Optional<ConfigurationInstance> configuration = getConfiguration(event);

      ComponentIdentifier operationIdentifier =
          new ComponentIdentifier.Builder().withName(operationModel.getName())
              .withNamespace(extensionModel.getName().toLowerCase()).build();
      Map<String, Object> originalParameterMap = this.resolverSet.resolve(event).asMap();
      PolicyProvider policyProvider = policyManager.lookupPolicyProvider();
      Optional<Policy> policy =
          policyProvider.findOperationPolicy(event.getContext().getId(), operationIdentifier);
      if (policy.isPresent()) {
        Optional<PolicyOperationParametersTransformer> policyOperationParametersTransformer =
            policyManager.lookupOperationParametersTransformer(operationIdentifier);
        Event policyEvent;
        if (policyOperationParametersTransformer.isPresent()) {
          Message message = policyOperationParametersTransformer.get().fromParametersToMessage(originalParameterMap);
          policyEvent = Event.builder(event).message((InternalMessage) message).build();
        } else {
          policyEvent = event;
        }
        AtomicReference<Event> operationResult = new AtomicReference<>();
        NextOperation executeOperationFunction = (policyExecuteNextEvent) -> {
          try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.putAll(originalParameterMap);
            if (policyOperationParametersTransformer.isPresent()) {
              parameters.putAll(policyOperationParametersTransformer.get()
                  .fromMessageToParameters(policyExecuteNextEvent.getMessage()));
            }
            ExecutionContextAdapter operationContext = createExecutionContext(configuration, parameters, event);
            MuleEvent muleEvent = doProcess(policyExecuteNextEvent, operationContext);
            Event resultEvent = (Event) muleEvent;
            operationResult.set(resultEvent);
            return resultEvent;
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        };
        policy.get().process(policyEvent, executeOperationFunction);
        return operationResult.get();
      } else {
        ExecutionContextAdapter operationContext = createExecutionContext(configuration, originalParameterMap, event);
        MuleEvent muleEvent = doProcess(event, operationContext);
        return (Event) muleEvent;
      }
    }, MuleException.class, e -> {
      throw new DefaultMuleException(e);
    });
  }

  protected org.mule.runtime.api.message.MuleEvent doProcess(Event event, ExecutionContextAdapter operationContext)
      throws MuleException {
    Object result = executeOperation(operationContext, event);
    MuleEvent resultEvent = returnDelegate.asReturnValue(result, operationContext);
    return resultEvent;
  }

  private Object executeOperation(ExecutionContextAdapter operationContext, Event event) throws MuleException {
    try {
      return executionMediator.execute(operationExecutor, operationContext);
    } catch (MessagingException e) {
      if (e.getEvent() == null) {
        throw wrapInMessagingException(event, e);
      }
      throw e;
    } catch (Throwable e) {
      throw wrapInMessagingException(event, e);
    }
  }

  private MessagingException wrapInMessagingException(Event event, Throwable e) {
    return new MessagingException(createStaticMessage(e.getMessage()), event, e, this);
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
    initialiseIfNeeded(operationExecutor, true, muleContext);
  }

  private ReturnDelegate createReturnDelegate() {
    if (isVoid(operationModel)) {
      return VoidReturnDelegate.INSTANCE;
    }

    return !isTargetPresent() ? new ValueReturnDelegate(muleContext) : new TargetReturnDelegate(target, muleContext);
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
    final MetadataContext metadataContext = getMetadataContext();
    return withContextClassLoader(getClassLoader(this.extensionModel),
                                  () -> entityMetadataMediator.getEntityKeys(metadataContext));
  }

  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(MetadataKey key) throws MetadataResolvingException {
    final MetadataContext metadataContext = getMetadataContext();
    return withContextClassLoader(getClassLoader(this.extensionModel),
                                  () -> entityMetadataMediator.getEntityMetadata(metadataContext, key));
  }

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel, operationModel, connectionManager);
  }

  /**
   * Validates that the {@link #operationModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalSourceException If the validation fails
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
}
