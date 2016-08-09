/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.lifecycle.EmptyLifecycleCallback;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.util.ClassUtils;

import java.beans.ExceptionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link FlowConstruct} that:
 * <ul>
 * <li>Is constructed with unique name and {@link MuleContext}.
 * <li>Uses a {@link MessageSource} as the source of messages.
 * <li>Uses a chain of {@link MessageProcessor}s to process messages.
 * <li>Has lifecycle and propagates this lifecycle to both {@link MessageSource} and {@link MessageProcessor}s in the correct
 * order depending on the lifecycle phase.
 * <li>Allows an {@link ExceptionListener} to be set.
 * </ul>
 * Implementations of <code>AbstractFlowConstuct</code> should implement {@link #validateConstruct()} validate the resulting
 * construct. Validation may include validation of the type of attributes of the {@link MessageSource}.
 * <p/>
 * Implementations may also implement {@link #doInitialise()}, {@link #doStart()}, {@link #doStop()} and {@link #doDispose()} if
 * they need to perform any action on lifecycle transitions.
 */
public abstract class AbstractFlowConstruct extends AbstractAnnotatedObject implements FlowConstruct, Lifecycle {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowConstruct.class);

  protected String name;
  protected MessagingExceptionHandler exceptionListener;
  protected final FlowConstructLifecycleManager lifecycleManager;
  protected final MuleContext muleContext;
  protected FlowConstructStatistics statistics;

  /**
   * The initial states that the flow can be started in
   */
  public static final String INITIAL_STATE_STOPPED = "stopped";
  public static final String INITIAL_STATE_STARTED = "started";

  /**
   * Determines the initial state of this flow when the mule starts. Can be 'stopped' or 'started' (default)
   */
  protected String initialState = INITIAL_STATE_STARTED;

  public AbstractFlowConstruct(String name, MuleContext muleContext) {
    this.muleContext = muleContext;
    this.name = name;
    this.lifecycleManager = new FlowConstructLifecycleManager(this, muleContext);
  }

  @Override
  public final void initialise() throws InitialisationException {
    try {
      if (exceptionListener == null) {
        this.exceptionListener = muleContext.getDefaultExceptionStrategy();
      }
      lifecycleManager.fireInitialisePhase((phaseName, object) -> {
        injectFlowConstructMuleContext(exceptionListener);
        initialiseIfInitialisable(exceptionListener);
        validateConstruct();
        doInitialise();
      });

    } catch (InitialisationException e) {
      throw e;
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public final void start() throws MuleException {
    // Check if Initial State is Stopped
    if (!isStopped() && initialState.equals(INITIAL_STATE_STOPPED)) {
      lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<FlowConstruct>());
      lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<FlowConstruct>());

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
  public MessagingExceptionHandler getExceptionListener() {
    return exceptionListener;
  }

  public void setExceptionListener(MessagingExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  public String getInitialState() {
    return initialState;
  }

  public void setInitialState(String initialState) {
    this.initialState = initialState;
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
    configureStatistics();
  }

  protected void configureStatistics() {
    statistics = new FlowConstructStatistics(getConstructType(), name);
    statistics.setEnabled(muleContext.getStatistics().isEnabled());
    muleContext.getStatistics().add(statistics);
  }

  protected void doStart() throws MuleException {
    // Empty template method
  }

  protected void doStop() throws MuleException {
    // Empty template method
  }

  protected void doDispose() {
    muleContext.getStatistics().remove(statistics);
  }

  /**
   * Validates configured flow construct
   *
   * @throws FlowConstructInvalidException if the flow construct does not pass validation
   */
  protected void validateConstruct() throws FlowConstructInvalidException {
    if (exceptionListener instanceof MessagingExceptionHandlerAcceptor) {
      if (!((MessagingExceptionHandlerAcceptor) exceptionListener).acceptsAll()) {
        throw new FlowConstructInvalidException(CoreMessages
            .createStaticMessage("Flow exception listener contains an exception strategy that doesn't handle all request,"
                + " Perhaps there's an exception strategy with a when attribute set but it's not part of a catch exception strategy"),
                                                this);
      }
    }
  }

  protected void injectFlowConstructMuleContext(Object candidate) {
    if (candidate instanceof FlowConstructAware) {
      ((FlowConstructAware) candidate).setFlowConstruct(this);
    }
    if (candidate instanceof MuleContextAware) {
      ((MuleContextAware) candidate).setMuleContext(muleContext);
    }
  }

  protected void injectExceptionHandler(Object candidate) {
    if (candidate instanceof MessagingExceptionHandlerAware) {
      ((MessagingExceptionHandlerAware) candidate).setMessagingExceptionHandler(this.getExceptionListener());
    }
  }

  @Override
  public String toString() {
    return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
  }

  protected void initialiseIfInitialisable(Object candidate) throws InitialisationException {
    initialiseIfNeeded(candidate);
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
