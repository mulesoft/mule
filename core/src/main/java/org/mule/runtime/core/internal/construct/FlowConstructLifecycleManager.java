/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_DISPOSED;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_INITIALISED;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_STARTED;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_STOPPED;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.api.notification.FlowConstructNotification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.privileged.lifecycle.SimpleLifecycleManager;

import org.slf4j.Logger;

/**
 * The lifecycle manager responsible for managing lifecycle transitions for a Mule service. The Mule service adds some additional
 * states, namely pause and resume. The lifecycle manager manages lifecycle notifications and logging as well.
 */
public class FlowConstructLifecycleManager extends SimpleLifecycleManager<FlowConstruct> {

  /**
   * logger used by this class
   */
  private static final Logger logger = getLogger(FlowConstructLifecycleManager.class);
  private NotificationDispatcher notificationFirer;

  public FlowConstructLifecycleManager(FlowConstruct flowConstruct, NotificationDispatcher notificationFirer) {
    super(flowConstruct.getName(), flowConstruct);
    this.notificationFirer = notificationFirer;
  }

  @Override
  public void fireInitialisePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException {
    checkPhase(Initialisable.PHASE_NAME);
    // TODO No pre notification
    if (logger.isInfoEnabled()) {
      logger.info("Initialising flow: " + getLifecycleObject().getName());
    }
    invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
    fireNotification(FLOW_CONSTRUCT_INITIALISED);
  }


  @Override
  public void fireStartPhase(LifecycleCallback<FlowConstruct> callback) throws MuleException {
    checkPhase(Startable.PHASE_NAME);
    if (logger.isInfoEnabled()) {
      logger.info("Starting flow: " + getLifecycleObject().getName());
    }
    // TODO No pre notification
    invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
    fireNotification(FLOW_CONSTRUCT_STARTED);
  }


  @Override
  public void fireStopPhase(LifecycleCallback<FlowConstruct> callback) throws MuleException {
    checkPhase(Stoppable.PHASE_NAME);
    if (logger.isInfoEnabled()) {
      logger.info("Stopping flow: " + getLifecycleObject().getName());
    }
    // TODO No pre notification
    invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
    fireNotification(FLOW_CONSTRUCT_STOPPED);
  }

  @Override
  public void fireDisposePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException {
    checkPhase(Disposable.PHASE_NAME);
    if (logger.isInfoEnabled()) {
      logger.info("Disposing flow: " + getLifecycleObject().getName());
    }
    // TODO No pre notification
    invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
    fireNotification(FLOW_CONSTRUCT_DISPOSED);
  }

  protected void fireNotification(int action) {
    notificationFirer.dispatch(new FlowConstructNotification(getLifecycleObject().getName(), action));
  }
}
