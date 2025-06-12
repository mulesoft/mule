/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_PERSISTED_FLOW_STATE;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.getSimpleName;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;

import static java.lang.String.format;
import static java.util.Optional.of;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.context.FlowStoppedPersistenceListener;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.processor.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link FlowConstruct} that:
 * <ul>
 * <li>Is constructed with unique name and {@link MuleContext}.
 * <li>Uses a {@link MessageSource} as the source of messages.
 * <li>Uses a chain of {@link Processor}s to process messages.
 * <li>Has lifecycle and propagates this lifecycle to both {@link MessageSource} and {@link Processor}s in the correct order
 * depending on the lifecycle phase.
 * <li>Allows an {@link ExceptionListener} to be set.
 * </ul>
 * Implementations of <code>AbstractFlowConstuct</code> should implement {@link #validateConstruct()} validate the resulting
 * construct. Validation may include validation of the type of attributes of the {@link MessageSource}.
 * <p/>
 * Implementations may also implement {@link #doInitialise()}, {@link #doStart()}, {@link #doStop()} and {@link #doDispose()} if
 * they need to perform any action on lifecycle transitions.
 */
public abstract class AbstractFlowConstruct extends AbstractExecutableComponent implements FlowConstruct, Lifecycle {

  public static final String FLOW_FLOW_CONSTRUCT_TYPE = "Flow";

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConstruct.class);

  private final FlowConstructLifecycleManager lifecycleManager;
  private final String name;
  private final FlowExceptionHandler exceptionListener;
  private volatile FlowConstructStatistics statistics;
  protected FlowStoppedPersistenceListener flowStoppedPersistenceListener;

  private FeatureFlaggingService featureFlaggingService;

  /**
   * Determines the initial state of this flow when the mule starts. Can be 'stopped' or 'started' (default)
   */
  private final String initialState;
  private Supplier<Boolean> isStatePersisted;

  public AbstractFlowConstruct(String name, MuleContext muleContext, Optional<FlowExceptionHandler> exceptionListener,
                               String initialState, FlowConstructStatistics statistics) {
    this.muleContext = muleContext;
    this.name = name;
    this.exceptionListener = exceptionListener.orElseGet(() -> muleContext.getDefaultErrorHandler(of(name)));
    this.initialState = initialState;
    try {
      this.lifecycleManager = new FlowConstructLifecycleManager(this, ((MuleContextWithRegistry) muleContext).getRegistry()
          .lookupObject(NotificationDispatcher.class));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
    this.statistics = statistics;
  }

  @Override
  public final void initialise() throws InitialisationException {
    try {
      lifecycleManager.fireInitialisePhase((phaseName, object) -> {
        initialiseIfNeeded(exceptionListener, muleContext);
        validateConstruct();
        doInitialise();
      });
    } catch (InitialisationException e) {
      safely((CheckedRunnable) this::dispose);
      throw e;
    } catch (MuleException e) {
      safely((CheckedRunnable) this::dispose);
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public final void start() throws MuleException {
    boolean usePersistedState =
        featureFlaggingService != null && featureFlaggingService.isEnabled(HONOUR_PERSISTED_FLOW_STATE)
            && isStatePersisted != null && isStatePersisted.get();
    // Check if Initial State is Stopped
    if (muleContext.isStarting() &&
        (!usePersistedState && initialState.equals(INITIAL_STATE_STOPPED)
            || flowStoppedPersistenceListener != null && !flowStoppedPersistenceListener.shouldStart())) {
      lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<>());
      lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<>());

      LOGGER.info("Flow " + name + " has not been started");
      return;
    }

    lifecycleManager.fireStartPhase((phaseName, object) -> {
      doStartProcessingStrategy();
      startIfStartable(exceptionListener);
      doStart();
    });
    if (flowStoppedPersistenceListener != null) {
      flowStoppedPersistenceListener.onStart();
    }
  }

  @Override
  public final void stop() throws MuleException {
    lifecycleManager.fireStopPhase((phaseName, object) -> {
      doStop();
      stopIfStoppable(exceptionListener);
      doStopProcessingStrategy();
    });
    if (flowStoppedPersistenceListener != null) {
      flowStoppedPersistenceListener.onStop();
    }
  }

  @Override
  public final void dispose() {
    try {
      if (isStarted()) {
        stop();
      }

      lifecycleManager.fireDisposePhase((phaseName, object) -> {
        disposeIfDisposable(exceptionListener);
        doDispose();
      });
    } catch (Exception e) {
      LOGGER.atError()
          .setCause(e)
          .log("Failed to stop service: {}", name);
    }
  }

  public boolean isStarted() {
    return lifecycleManager.getState().isStarted();
  }

  public boolean isStopped() {
    return lifecycleManager.getState().isStopped();
  }

  public boolean isStopping() {
    return lifecycleManager.getState().isStopping();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public FlowExceptionHandler getExceptionListener() {
    return exceptionListener;
  }

  public String getInitialState() {
    return initialState;
  }

  @Override
  public LifecycleState getLifecycleState() {
    return lifecycleManager.getState();
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public FlowConstructStatistics getStatistics() {
    return statistics;
  }

  protected void doInitialise() throws MuleException {
    if (this.exceptionListener instanceof ErrorHandler) {
      ((ErrorHandler) this.exceptionListener).setStatistics(statistics);
    }
  }

  public static FlowConstructStatistics createFlowStatistics(String flowName, AllStatistics statistics) {
    DefaultFlowConstructStatistics flowStatistics = new DefaultFlowConstructStatistics(FLOW_FLOW_CONSTRUCT_TYPE, flowName);
    flowStatistics.setEnabled(statistics.isEnabled());
    statistics.add(flowStatistics);
    return flowStatistics;
  }

  protected void doStartProcessingStrategy() throws MuleException {
    // Empty template method
  }

  protected void doInitialiseProcessingStrategy() throws MuleException {
    // Empty template method
  }

  protected void doStart() throws MuleException {
    // Empty template method
  }

  protected void doStopProcessingStrategy() throws MuleException {
    // Empty template method
  }

  protected void doStop() throws MuleException {
    // Empty template method
  }

  protected void doDispose() {
    muleContext.getStatistics().remove(statistics);
    statistics = null;
  }

  /**
   * Validates configured flow construct
   *
   * @throws FlowConstructInvalidException if the flow construct does not pass validation
   */
  protected void validateConstruct() throws FlowConstructInvalidException {
    if (exceptionListener instanceof MessagingExceptionHandlerAcceptor) {
      if (!((MessagingExceptionHandlerAcceptor) exceptionListener).acceptsAll()) {
        throw new FlowConstructInvalidException(createStaticMessage("Flow exception listener contains an exception strategy that doesn't handle all request,"
            + " Perhaps there's an exception strategy with a when attribute set but it's not part of a catch exception strategy"),
                                                this);
      }
    }
  }

  @Override
  public String getUniqueIdString() {
    return muleContext.getUniqueIdString();
  }

  @Override
  public String getServerId() {
    return muleContext.getId();
  }

  @Override
  public String toString() {
    return format("%s{%s}", getSimpleName(this.getClass()), getName());
  }

  protected void startIfStartable(Object candidate) throws MuleException {
    startIfNeeded(candidate);
  }

  protected void stopIfStoppable(Object candidate) throws MuleException {
    stopIfNeeded(candidate);
  }

  protected void disposeIfDisposable(Object candidate) {
    disposeIfNeeded(candidate, LOGGER);
  }

  public void addFlowStoppedListener(FlowStoppedPersistenceListener flowStoppedPersistenceListener) {
    this.flowStoppedPersistenceListener = flowStoppedPersistenceListener;
  }

  public void setIsStatePersisted(Supplier<Boolean> isStatePersisted) {
    this.isStatePersisted = isStatePersisted;
  }

  public void doNotPersist() {
    if (flowStoppedPersistenceListener != null) {
      flowStoppedPersistenceListener.doNotPersist();
    }
  }

  public void setFeatureFlaggingService(FeatureFlaggingService featureFlaggingService) {
    this.featureFlaggingService = featureFlaggingService;
  }
}
