/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STOPPED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.getSimpleName;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;

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
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.ExceptionListener;
import java.util.Optional;

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

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConstruct.class);

  private final FlowConstructLifecycleManager lifecycleManager;
  private final String name;
  private final FlowExceptionHandler exceptionListener;
  private volatile FlowConstructStatistics statistics;

  /**
   * Determines the initial state of this flow when the mule starts. Can be 'stopped' or 'started' (default)
   */
  private final String initialState;

  public AbstractFlowConstruct(String name, MuleContext muleContext, Optional<FlowExceptionHandler> exceptionListener,
                               String initialState, FlowConstructStatistics statistics) {
    this.muleContext = muleContext;
    this.name = name;
    this.exceptionListener = exceptionListener.orElse(muleContext.getDefaultErrorHandler(of(name)));
    this.initialState = initialState;
    try {
      this.lifecycleManager = new FlowConstructLifecycleManager(this, ((MuleContextWithRegistries) muleContext).getRegistry()
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
      safely(() -> dispose());
      throw e;
    } catch (MuleException e) {
      safely(() -> dispose());
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public final void start() throws MuleException {
    // Check if Initial State is Stopped
    if (!isStopped() && initialState.equals(INITIAL_STATE_STOPPED)) {
      lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<>());
      lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<>());

      LOGGER.info("Flow " + name + " has not been started (initial state = 'stopped')");
      return;
    }

    lifecycleManager.fireStartPhase((phaseName, object) -> {
      startIfStartable(exceptionListener);
      doStart();
    });
  }

  @Override
  public final void stop() throws MuleException {
    lifecycleManager.fireStopPhase((phaseName, object) -> {
      doStop();
      stopIfStoppable(exceptionListener);
    });
  }

  @Override
  public final void dispose() {
    try {
      if (isStarted()) {
        stop();
      }

      lifecycleManager.fireDisposePhase((phaseName, object) -> {
        doDispose();
        disposeIfDisposable(exceptionListener);
      });
    } catch (MuleException e) {
      LOGGER.error("Failed to stop service: " + name, e);
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

  protected void doInitialise() throws MuleException {}

  public static FlowConstructStatistics createFlowStatistics(String flowName, MuleContext muleContext) {
    DefaultFlowConstructStatistics statistics = new DefaultFlowConstructStatistics("Flow", flowName);
    statistics.setEnabled(muleContext.getStatistics().isEnabled());
    muleContext.getStatistics().add(statistics);
    return statistics;
  }

  protected void doStart() throws MuleException {
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

  /**
   * @return the type of construct being created, e.g. "Flow"
   */
  public abstract String getConstructType();
}
