/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleExceptionInfo;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.logging.LogConfigChangeSubject;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Manager for handling message processing troubleshooting data.
 */
public class MessageProcessingFlowTraceManager extends LocationExecutionContextProvider
    implements FlowTraceManager, Initialisable, Disposable {

  public static final String FLOW_STACK_INFO_KEY = MuleExceptionInfo.FLOW_STACK_INFO_KEY;

  private final FlowNotificationTextDebugger pipelineProcessorDebugger;
  private final MessageProcessorTextDebugger messageProcessorTextDebugger;

  private ServerNotificationManager notificationManager;

  private volatile boolean listenersAdded = false;
  private final PropertyChangeListener logConfigChangeListener = evt -> handleNotificationListeners();


  public MessageProcessingFlowTraceManager() {
    messageProcessorTextDebugger = new MessageProcessorTextDebugger(this);
    pipelineProcessorDebugger = new FlowNotificationTextDebugger(this);
  }

  @Override
  public void initialise() throws InitialisationException {
    withLoggerContext(context -> ((LogConfigChangeSubject) context).registerLogConfigChangeListener(logConfigChangeListener));
    handleNotificationListeners();
  }

  @Override
  public void dispose() {
    withLoggerContext(context -> ((LogConfigChangeSubject) context).unregisterLogConfigChangeListener(logConfigChangeListener));
    removeNotificationListeners();
  }

  protected void withLoggerContext(Consumer<LoggerContext> action) {
    LoggerContext context = LogManager.getContext(false);
    if (context != null && context instanceof LogConfigChangeSubject) {
      action.accept(context);
    }
  }

  protected synchronized void handleNotificationListeners() {
    if (!notificationManager.isDisposed()) {
      if (!listenersAdded) {
        notificationManager.addListener(messageProcessorTextDebugger);
        notificationManager.addListener(pipelineProcessorDebugger);
        listenersAdded = true;
      }
    }
  }

  protected synchronized void removeNotificationListeners() {
    if (listenersAdded && !notificationManager.isDisposed()) {
      notificationManager.removeListener(messageProcessorTextDebugger);
      notificationManager.removeListener(pipelineProcessorDebugger);
      listenersAdded = false;
    }
  }

  /**
   * Callback method for when a message processor is about to be invoked.
   *
   * @see DefaultFlowCallStack#setCurrentProcessorPath(String)
   *
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onMessageProcessorNotificationPreInvoke(MessageProcessorNotification notification) {
    FlowCallStack flowCallStack = ((CoreEvent) notification.getEvent()).getFlowCallStack();
    if (flowCallStack != null) {
      ((DefaultFlowCallStack) flowCallStack).setCurrentProcessorPath(notification.getComponent().getRepresentation());
    }
  }

  /**
   * Callback method for when a flow or sub-flow called from a {@code flow-ref} component has been completed.
   *
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onPipelineNotificationComplete(PipelineMessageNotification notification) {
    onFlowComplete(notification.getInfo());
  }

  /**
   * Callback method for when a flow or sub-flow is about to be called from a {@code flow-ref}.
   *
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onPipelineNotificationStart(PipelineMessageNotification notification) {
    onFlowStart(notification.getInfo(), notification.getResourceIdentifier());
  }

  @Override
  public void onFlowStart(EnrichedNotificationInfo notificationInfo, String flowName) {
    // Nothing to do
  }

  @Override
  public void onFlowComplete(EnrichedNotificationInfo notificationInfo) {
    // Nothing to do
  }

  @Override
  public Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    final Map<String, Object> info = new HashMap<>();
    info.putIfAbsent(FLOW_STACK_INFO_KEY, ((CoreEvent) notificationInfo.getEvent()).getFlowCallStack().clone());
    return info;
  }

  @Override
  public void putContextInfo(MuleExceptionInfo info, EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    if (info.getFlowStack() != null) {
      info.setFlowStack(((CoreEvent) notificationInfo.getEvent()).getFlowCallStack().clone());
    }
  }

  @Inject
  public void setNotificationManager(ServerNotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }
}
