/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.core.api.config.DefaultMuleConfiguration.isFlowTrace;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;
import org.mule.runtime.core.internal.logging.LogConfigChangeSubject;

import java.beans.PropertyChangeListener;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Manager for handling message processing troubleshooting data.
 */
public class MessageProcessingFlowTraceManager extends LocationExecutionContextProvider
    implements FlowTraceManager, MuleContextAware, Initialisable, Disposable {

  public static final String FLOW_STACK_INFO_KEY = "FlowStack";

  private final FlowNotificationTextDebugger pipelineProcessorDebugger;
  private final MessageProcessorTextDebugger messageProcessorTextDebugger;

  private MuleContext muleContext;

  private PropertyChangeListener logConfigChangeListener = evt -> handleNotificationListeners();

  public MessageProcessingFlowTraceManager() {
    messageProcessorTextDebugger = new MessageProcessorTextDebugger(this);
    pipelineProcessorDebugger = new FlowNotificationTextDebugger(this);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    LoggerContext context = LogManager.getContext(false);
    if (context != null && context instanceof LogConfigChangeSubject) {
      ((LogConfigChangeSubject) context).registerLogConfigChangeListener(logConfigChangeListener);
    }

    handleNotificationListeners();
  }

  @Override
  public void dispose() {
    LoggerContext context = LogManager.getContext(false);
    if (context != null && context instanceof LogConfigChangeSubject) {
      ((LogConfigChangeSubject) context).unregisterLogConfigChangeListener(logConfigChangeListener);
    }

    removeNotificationListeners();
  }

  protected void handleNotificationListeners() {
    if (!muleContext.getNotificationManager().isDisposed()) {
      if (DefaultMuleConfiguration.isFlowTrace()) {
        muleContext.getNotificationManager().addListener(messageProcessorTextDebugger);
        muleContext.getNotificationManager().addListener(pipelineProcessorDebugger);
      } else {
        removeNotificationListeners();
      }
    }
  }

  protected void removeNotificationListeners() {
    if (!muleContext.getNotificationManager().isDisposed()) {
      muleContext.getNotificationManager().removeListener(messageProcessorTextDebugger);
      muleContext.getNotificationManager().removeListener(pipelineProcessorDebugger);
    }
  }

  /**
   * Callback method for when a message processor is about to be invoked.
   * <p/>
   * Updates the internal state of the event's {@link ProcessorsTrace} and {@link FlowCallStack} accordingly.
   *
   * @see DefaultProcessorsTrace#addExecutedProcessors(String)
   * @see DefaultFlowCallStack#setCurrentProcessorPath(String)
   *
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onMessageProcessorNotificationPreInvoke(MessageProcessorNotification notification) {
    String resolveProcessorRepresentation =
        resolveProcessorRepresentation(muleContext.getConfiguration().getId(),
                                       notification.getComponent().getLocation() != null
                                           ? notification.getComponent().getLocation().getLocation()
                                           : null,
                                       notification.getComponent());

    EventContext eventContext = notification.getEventContext();
    if (eventContext != null) {
      ((DefaultProcessorsTrace) ((BaseEventContext) eventContext).getProcessorsTrace())
          .addExecutedProcessors(resolveProcessorRepresentation);
    }

    FlowCallStack flowCallStack = ((CoreEvent) notification.getEvent()).getFlowCallStack();
    if (flowCallStack != null) {
      ((DefaultFlowCallStack) flowCallStack).setCurrentProcessorPath(resolveProcessorRepresentation);
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
    FlowCallStack flowCallStack = ((CoreEvent) notificationInfo.getEvent()).getFlowCallStack();
    if (flowCallStack instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) flowCallStack).push(new FlowStackElement(flowName, null));
    }
  }

  @Override
  public void onFlowComplete(EnrichedNotificationInfo notificationInfo) {
    FlowCallStack flowCallStack = ((CoreEvent) notificationInfo.getEvent()).getFlowCallStack();
    if (flowCallStack instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) flowCallStack).pop();
    }
  }

  @Override
  public Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    FlowCallStack flowCallStack = ((CoreEvent) notificationInfo.getEvent()).getFlowCallStack();
    if (isFlowTrace()) {
      return singletonMap(FLOW_STACK_INFO_KEY, flowCallStack.toString());
    } else {
      return emptyMap();
    }
  }
}
