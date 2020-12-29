/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChainExecutor;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Default implementation of {@link ExecutionContextAdapter} which adds additional information which is relevant to this
 * implementation of the extensions-api, even though it's not part of the API itself
 *
 * @since 3.7.0
 */
public class DefaultExecutionContext<M extends ComponentModel> implements ExecutionContextAdapter<M> {

  private final ExtensionModel extensionModel;
  private final Optional<ConfigurationInstance> configuration;
  private final Map<String, Object> parameters;
  private final Map<String, Object> variables = new SmallMap<>();
  private final M componentModel;
  private final MuleContext muleContext;
  private CoreEvent event;
  private SecurityContext securityContext;
  private final CursorProviderFactory cursorProviderFactory;
  private final CursorComponentDecoratorFactory componentDecoratorFactory;
  private final StreamingManager streamingManager;
  private final Optional<TransactionConfig> transactionConfig;
  private final Component component;
  private final RetryPolicyTemplate retryPolicyTemplate;
  private Scheduler currentScheduler;

  /**
   * Creates a new instance with the given state
   *
   * @param configuration         the {@link ConfigurationInstance} that the operation will use
   * @param parameters            the parameters that the operation will use
   * @param componentModel        the {@link ComponentModel} for the component being executed
   * @param event                 the current {@link CoreEvent}
   * @param cursorProviderFactory the {@link CursorProviderFactory} that was configured on the executed component
   * @param streamingManager      the application's {@link StreamingManager}
   * @param component             the {@link Component component} executing
   * @param retryPolicyTemplate   the reconnection strategy to use in case of connectivity problems
   * @param muleContext           the current {@link MuleContext}
   */
  public DefaultExecutionContext(ExtensionModel extensionModel,
                                 Optional<ConfigurationInstance> configuration,
                                 Map<String, Object> parameters,
                                 M componentModel,
                                 CoreEvent event,
                                 CursorProviderFactory cursorProviderFactory,
                                 CursorComponentDecoratorFactory componentDecoratorFactory,
                                 StreamingManager streamingManager,
                                 Component component,
                                 RetryPolicyTemplate retryPolicyTemplate,
                                 Scheduler currentScheduler,
                                 Optional<TransactionConfig> transactionConfig,
                                 MuleContext muleContext) {
    this.extensionModel = extensionModel;
    this.configuration = configuration;
    this.event = event;
    this.securityContext = event.getSecurityContext();
    this.componentModel = componentModel;
    this.parameters = parameters;
    this.cursorProviderFactory = cursorProviderFactory;
    this.componentDecoratorFactory = componentDecoratorFactory;
    this.streamingManager = streamingManager;
    this.muleContext = muleContext;
    this.component = component;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.currentScheduler = currentScheduler;
    this.transactionConfig = transactionConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return configuration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasParameter(String parameterName) {
    return parameters.containsKey(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getParameter(String parameterName) {
    if (hasParameter(parameterName)) {
      return (T) parameters.get(parameterName);
    } else {
      throw new NoSuchElementException(parameterName);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getParameterOrDefault(String parameterName, T defaultValue) {
    if (hasParameter(parameterName)) {
      return (T) parameters.get(parameterName);
    }

    return defaultValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getVariable(String key) {
    return (T) variables.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object setVariable(String key, Object value) {
    return variables.put(requireNonNull(key, "null keys are not allowed"), requireNonNull(value, "null values are not allowed"));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T removeVariable(String key) {
    return (T) variables.remove(requireNonNull(key, "null keys are not allowed"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CoreEvent getEvent() {
    return event;
  }

  @Override
  public void changeEvent(CoreEvent updated) {
    requireNonNull(event);
    securityContext = updated.getSecurityContext();
    // Prevents possible lack of SdkContext if this context was generated by an Interceptor
    // (see MULE-19088, ReactiveInterceptorAdapter#setInternalParamsForNotParamResolver and ComponentMessageProcessor#shouldUsePrecalculatedContext)
    this.getParameters().values().forEach(parameterValue -> {
      if (parameterValue instanceof ImmutableProcessorChainExecutor) {
        CoreEvent originalEvent = ((ImmutableProcessorChainExecutor) parameterValue).getOriginalEvent();
        if (originalEvent.getContext().getId().equals(event.getContext().getId())) {
          ((InternalEvent) originalEvent).setSdkInternalContext(((InternalEvent) updated).getSdkInternalContext());
        }
      }
    });
    event = updated;
  }

  @Override
  public void setSecurityContext(SecurityContext securityContext) {
    requireNonNull(securityContext);
    this.securityContext = securityContext;
  }

  @Override
  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  @Override
  public void setCurrentScheduler(Scheduler currentScheduler) {
    this.currentScheduler = currentScheduler;
  }

  @Override
  public Scheduler getCurrentScheduler() {
    return currentScheduler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public M getComponentModel() {
    return componentModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorProviderFactory getCursorProviderFactory() {
    return cursorProviderFactory;
  }

  @Override
  public CursorComponentDecoratorFactory getComponentDecoratorFactory() {
    return componentDecoratorFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<TransactionConfig> getTransactionConfig() {
    return transactionConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamingManager getStreamingManager() {
    return streamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Component getComponent() {
    return component;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ComponentLocation getComponentLocation() {
    return component.getLocation();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<RetryPolicyTemplate> getRetryPolicyTemplate() {
    return ofNullable(retryPolicyTemplate);
  }
}
