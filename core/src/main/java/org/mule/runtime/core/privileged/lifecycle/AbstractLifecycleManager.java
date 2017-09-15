/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.lifecycle;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.privileged.transport.LegacyConnector;
import org.mule.runtime.core.internal.lifecycle.DefaultLifecycleState;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a base implementation of the {@link org.mule.runtime.core.api.lifecycle.LifecycleManager} interface and provides almost
 * all the plumbing required to write a {@link org.mule.runtime.core.api.lifecycle.LifecycleManager} implementation. This class
 * handles the tracking ofg the phases, transition validation and checking state.
 *
 * @param <O> The object type being managed by this {@link org.mule.runtime.core.api.lifecycle.LifecycleManager}
 * @since 3.0
 */
public abstract class AbstractLifecycleManager<O> implements LifecycleManager {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(AbstractLifecycleManager.class);

  protected String lifecycleManagerId;
  protected String currentPhase = NotInLifecyclePhase.PHASE_NAME;
  protected String executingPhase = null;
  private Set<String> directTransitions = new HashSet<>();
  protected Set<String> phaseNames = new LinkedHashSet<>(4);
  protected Set<String> completedPhases = new LinkedHashSet<>(4);
  protected O object;
  protected LifecycleState state;
  private String lastPhaseExecuted;
  private boolean lastPhaseExecutionFailed;

  private TreeMap<String, LifecycleCallback> callbacks = new TreeMap<>();

  public AbstractLifecycleManager(String id, O object) {
    lifecycleManagerId = id;
    this.object = object;
    state = createLifecycleState();

    currentPhase = NotInLifecyclePhase.PHASE_NAME;
    completedPhases.add(currentPhase);
    registerTransitions();
  }

  protected abstract void registerTransitions();

  public void registerLifecycleCallback(String phaseName, LifecycleCallback<O> callback) {
    callbacks.put(phaseName, callback);
  }

  protected LifecycleState createLifecycleState() {
    return new DefaultLifecycleState(this);
  }

  protected void addDirectTransition(String phase1, String phase2) {
    directTransitions.add(phase1 + "-" + phase2);
    phaseNames.add(phase1);
    phaseNames.add(phase2);
  }

  @Override
  public void checkPhase(String name) throws IllegalStateException {
    if (lastPhaseExecutionFailed) {
      return;
    }
    if (executingPhase != null) {
      if (name.equalsIgnoreCase(executingPhase)) {
        throw new IllegalStateException("Phase '" + name + "' is already currently being executed");
      } else {
        throw new IllegalStateException("Cannot fire phase '" + name + "', currently executing lifecycle phase: "
            + executingPhase);
      }
    }

    if (name.equalsIgnoreCase(currentPhase)) {
      throw new IllegalStateException("Already in lifecycle phase '" + name + "', cannot fire the same phase twice");
    }


    if (!phaseNames.contains(name)) {
      throw new IllegalStateException("Phase does not exist: " + name);
    } else {
      if (isDirectTransition(name)) {
        return;
      }

      throw new IllegalStateException("Lifecycle Manager '" + lifecycleManagerId + "' phase '" + currentPhase
          + "' does not support phase '" + name + "'");
    }
  }

  public O getLifecycleObject() {
    return object;
  }

  @Override
  public void fireLifecycle(String phase) throws LifecycleException {
    checkPhase(phase);
    invokePhase(phase, object, callbacks.get(phase));
  }

  protected void invokePhase(String phase, Object object, LifecycleCallback callback) throws LifecycleException {
    try {
      this.lastPhaseExecuted = phase;
      setExecutingPhase(phase);
      callback.onTransition(phase, object);
      setCurrentPhase(phase);
      lastPhaseExecutionFailed = false;
    }
    // In the case of a connection exception, trigger the reconnection strategy.
    catch (ConnectException ce) {
      lastPhaseExecutionFailed = true;
      doOnConnectException(ce);
    } catch (LifecycleException le) {
      lastPhaseExecutionFailed = true;
      throw le;
    } catch (Exception e) {
      lastPhaseExecutionFailed = true;
      throw new LifecycleException(e, object);
    } finally {
      setExecutingPhase(null);
    }
  }

  protected void doOnConnectException(ConnectException ce) throws LifecycleException {
    MuleContext muleContext = ((LegacyConnector) ce.getFailed()).getMuleContext();
    muleContext.getExceptionListener().handleException(ce);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyPhase(Object object, String startPhase, String toPhase) throws LifecycleException {
    if (startPhase == null || toPhase == null) {
      throw new IllegalArgumentException("toPhase and fromPhase must be null");
    }
    if (!phaseNames.contains(startPhase)) {
      throw new IllegalArgumentException("fromPhase '" + startPhase + "' not a valid phase.");
    }
    if (!phaseNames.contains(toPhase)) {
      throw new IllegalArgumentException("toPhase '" + startPhase + "' not a valid phase.");
    }
    boolean started = false;
    for (String phaseName : phaseNames) {
      if (started) {
        invokePhase(object, phaseName);
      }
      if (toPhase.equals(phaseName)) {
        break;
      }
      if (phaseName.equals(startPhase)) {
        started = true;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyCompletedPhases(Object object) throws LifecycleException {
    String lastPhase = NotInLifecyclePhase.PHASE_NAME;
    for (String phase : completedPhases) {
      if (isDirectTransition(lastPhase, phase)) {
        invokePhase(object, phase);
        lastPhase = phase;
      }
    }
  }

  private void invokePhase(Object object, String phase) throws LifecycleException {
    try {
      callbacks.get(phase).onTransition(phase, object);
    } catch (MuleException e) {
      throw new LifecycleException(e, object);
    }
  }

  @Override
  public boolean isDirectTransition(String destinationPhase) {
    return isDirectTransition(getCurrentPhase(), destinationPhase);
  }

  protected boolean isDirectTransition(String startPhase, String endPhase) {
    String key = startPhase + "-" + endPhase;
    return directTransitions.contains(key);
  }

  @Override
  public String getCurrentPhase() {
    return currentPhase;
  }

  protected void setCurrentPhase(String currentPhase) {
    this.currentPhase = currentPhase;
    completedPhases.add(currentPhase);
    // remove irrelevant phases
    if (currentPhase.equals(Stoppable.PHASE_NAME)) {
      completedPhases.remove(Startable.PHASE_NAME);
    } else if (currentPhase.equals(Startable.PHASE_NAME)) {
      completedPhases.remove(Stoppable.PHASE_NAME);
    } else if (currentPhase.equals(Disposable.PHASE_NAME)) {
      completedPhases.remove(Initialisable.PHASE_NAME);
    }

    notifyTransition(currentPhase);

  }

  @Override
  public String getExecutingPhase() {
    return executingPhase;
  }

  protected void setExecutingPhase(String executingPhase) {
    this.executingPhase = executingPhase;
  }

  /**
   * Allows any for any state adjustments in sub classes. For example, it may be necessary to remove a state from the
   * 'completedPhases' collection once a transition occurs. This is only necessary for a Lifecycle Manager that introduces a new
   * phase pair.
   *
   * @param phase the currently completed phase
   */
  protected void notifyTransition(String phase) {
    // do nothing
  }

  @Override
  public void reset() {
    completedPhases.clear();
    setExecutingPhase(null);
    setCurrentPhase(NotInLifecyclePhase.PHASE_NAME);
    completedPhases.add(getCurrentPhase());
  }

  @Override
  public boolean isPhaseComplete(String phaseName) {
    return completedPhases.contains(phaseName);
  }

  @Override
  public LifecycleState getState() {
    return state;
  }

  /**
   * @return the last phase executed name. It may be null if no phase was executed.
   */
  public String getLastPhaseExecuted() {
    return lastPhaseExecuted;
  }

  /**
   * @return true if the last phase execution failed. False otherwise.
   */
  public boolean isLastPhaseExecutionFailed() {
    return lastPhaseExecutionFailed;
  }
}
