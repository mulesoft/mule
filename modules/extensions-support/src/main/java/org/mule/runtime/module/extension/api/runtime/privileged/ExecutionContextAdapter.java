/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.Optional;

/**
 * Adapter interface which expands the contract of {@link ExecutionContext} which functionality that is internal to this
 * implementation of the extensions API and shouldn't be accessible for the extensions themselves.
 * <p/>
 * Among other things, it adds the concept of variables, which are key-value pairs in order to contain state that is not specific
 * to the operation but to the extensions framework itself. It's not to contain operation parameters as its
 * {@link #getParameter(String)} counter part. It's meant for things like connection pointers, state to be shared between
 * {@link Interceptor interceptors} and {@link CompletableComponentExecutor operation executors}, etc.
 *
 * @since 3.7.0
 */
@NoImplement
public interface ExecutionContextAdapter<M extends ComponentModel> extends EventedExecutionContext<M> {

  /**
   * Returns the value associated with the {@code key}
   *
   * @param key the variable's key
   * @param <T> the generic type for the value
   * @return the value associated with {@code key} or {@code null} if no such variable was registered.
   */
  <T> T getVariable(String key);

  /**
   * Sets a variable of the given {@code key} and {@code value}.
   *
   * @param key   the variable's key. Cannot be {@code null}
   * @param value the associated value. Cannot be {@code null}
   * @return the value previously associated with the {@code key} or {@code null} if no such association existed.
   */
  Object setVariable(String key, Object value);

  /**
   * Removes the variable value associated with {@code key}.
   *
   * @param key the variable's key. Cannot be {@code null}
   * @param <T> the generic type for the removed value
   * @return the value that was associated with the {@code key} or {@code null} if no such association existed
   */
  <T> T removeVariable(String key);

  /**
   * @return an {@link Optional} {@link TransactionConfig} if the operation is transactional
   */
  Optional<TransactionConfig> getTransactionConfig();

  /**
   * @return The {@link MuleContext} on which the operation is being executed
   * @deprecated Use other getters form this to get the required object.
   */
  @Deprecated(forRemoval = true, since = "4.10")
  MuleContext getMuleContext();

  /**
   * @return the {@link ArtifactEncoding} of the artifact of the executing component.
   * @since 4.10
   */
  ArtifactEncoding getArtifactEncoding();

  /**
   * @return the {@link ServerNotificationManager} of the artifact of the executing component.
   * @since 4.10
   */
  ServerNotificationManager getNotificationManager();

  /**
   * @return The {@link CursorProviderFactory} configured on the executing component
   */
  <T> CursorProviderFactory<T> getCursorProviderFactory();

  /**
   * @return The application's {@link StreamingManager}
   */
  StreamingManager getStreamingManager();

  /**
   * @return The {@link Component} executing
   * @since 4.1
   */
  Component getComponent();

  /**
   * @return The {@link ComponentLocation} of the executing component
   * @deprecated use {@link #getComponent().getLocation()}
   */
  @Deprecated
  ComponentLocation getComponentLocation();

  /**
   * @return The reconnection strategy to use in case of connectivity problems
   */
  Optional<RetryPolicyTemplate> getRetryPolicyTemplate();

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @param securityContext the context for this session or null if the request is not secure.
   */
  void setSecurityContext(SecurityContext securityContext);

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @return the context for this session or null if the request is not secure.
   */
  SecurityContext getSecurityContext();

  /**
   * Gets the security manager used by this Mule instance to authenticate and authorise incoming and outgoing event traffic and
   * service invocations
   *
   * @return the security manager used by the Mule instance to authenticate and authorise incoming and outgoing event traffic and
   *         service invocations
   */
  SecurityManager getSecurityManager();

  /**
   * @param currentScheduler the scheduler that was determined by the {@link ProcessingStrategy} to execute this component.
   */
  void setCurrentScheduler(Scheduler currentScheduler);

  /**
   * @return the scheduler that was determined by the {@link ProcessingStrategy} to execute this component.
   */
  Scheduler getCurrentScheduler();
}
