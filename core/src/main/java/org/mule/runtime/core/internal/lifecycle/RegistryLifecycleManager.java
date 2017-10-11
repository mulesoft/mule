/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextDisposePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStartPhase;
import org.mule.runtime.core.internal.lifecycle.phases.MuleContextStopPhase;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.AbstractRegistryBroker;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.privileged.lifecycle.AbstractLifecycleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class RegistryLifecycleManager extends AbstractLifecycleManager<Registry> {


  protected Map<String, LifecyclePhase> phases = new HashMap<>();
  protected SortedMap<String, LifecycleCallback> callbacks = new TreeMap<>();
  protected MuleContext muleContext;
  private final LifecycleInterceptor lifecycleInterceptor;

  public RegistryLifecycleManager(String id, Registry object, MuleContext muleContext,
                                  LifecycleInterceptor lifecycleInterceptor) {
    super(id, object);
    this.muleContext = muleContext;
    this.lifecycleInterceptor = lifecycleInterceptor;

    registerPhases(object);
  }

  protected void registerPhases(Registry object) {
    final RegistryLifecycleCallback<Object> callback = new RegistryLifecycleCallback<>(this);
    final LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();

    registerPhase(NotInLifecyclePhase.PHASE_NAME, new NotInLifecyclePhase(), emptyCallback);
    registerPhase(Initialisable.PHASE_NAME, new MuleContextInitialisePhase(), callback);
    registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), emptyCallback);
    registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), emptyCallback);
    registerPhase(Disposable.PHASE_NAME, new MuleContextDisposePhase(), callback);
  }

  @Override
  protected void registerTransitions() {
    addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
    addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);

    // start stop
    addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
    addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
    // Dispose can be called from init or stopped
    addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
    addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);
    addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);

  }

  protected void registerPhase(String phaseName, LifecyclePhase phase) {
    registerPhase(phaseName, phase, new RegistryLifecycleCallback(this));
  }

  protected void registerPhase(String phaseName, LifecyclePhase phase, LifecycleCallback callback) {
    if (callback instanceof HasLifecycleInterceptor) {
      if (Initialisable.PHASE_NAME.equals(phaseName) || Disposable.PHASE_NAME.equals(phaseName)) {
        ((HasLifecycleInterceptor) callback).setLifecycleInterceptor(lifecycleInterceptor);
      }
      if (Startable.PHASE_NAME.equals(phaseName) || Stoppable.PHASE_NAME.equals(phaseName)) {
        ((HasLifecycleInterceptor) callback).setLifecycleInterceptor(lifecycleInterceptor);
      }
    }

    phaseNames.add(phaseName);
    callbacks.put(phaseName, callback);
    phases.put(phaseName, phase);
  }

  @Override
  public void fireLifecycle(String destinationPhase) throws LifecycleException {
    checkPhase(destinationPhase);
    if (isDirectTransition(destinationPhase) || isLastPhaseExecutionFailed()) {
      // transition to phase without going through other phases first
      invokePhase(destinationPhase, object, callbacks.get(destinationPhase));
    } else {
      // Call all phases to including the destination phase
      boolean start = false;
      for (String phase : phaseNames) {
        if (start) {
          invokePhase(phase, object, callbacks.get(phase));
          if (phase.equals(destinationPhase)) {
            break;
          }
        }
        if (phase.equals(getCurrentPhase())) {
          start = true;
        }
      }
    }
  }

  @Override
  protected void doOnConnectException(ConnectException ce) throws LifecycleException {
    throw new LifecycleException(ce, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyPhase(Object object, String startPhase, String toPhase) throws LifecycleException {
    // TODO i18n
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
        doApplyLifecycle(object, phaseName);
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
        doApplyLifecycle(object, phase);
        lastPhase = phase;
      }
    }
  }

  private void doApplyLifecycle(Object object, String phase) throws LifecycleException {
    LifecyclePhase lp = phases.get(phase);
    lifecycleInterceptor.beforePhaseExecution(lp, object);
    try {
      lp.applyLifecycle(object);
      lifecycleInterceptor.afterPhaseExecution(lp, object, empty());
    } catch (Exception e) {
      lifecycleInterceptor.afterPhaseExecution(lp, object, of(e));
      throw e;
    }
  }
}
