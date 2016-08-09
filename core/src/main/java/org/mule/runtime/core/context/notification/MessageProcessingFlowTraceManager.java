/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.execution.LocationExecutionContextProvider;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.logging.LogConfigChangeSubject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
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

  private PropertyChangeListener logConfigChangeListener = new PropertyChangeListener() {

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      handleNotificationListeners();
    }
  };

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
    if (DefaultMuleConfiguration.isFlowTrace()) {
      muleContext.getNotificationManager().addListener(messageProcessorTextDebugger);
      muleContext.getNotificationManager().addListener(pipelineProcessorDebugger);
    } else {
      removeNotificationListeners();
    }
  }

  protected void removeNotificationListeners() {
    muleContext.getNotificationManager().removeListener(messageProcessorTextDebugger);
    muleContext.getNotificationManager().removeListener(pipelineProcessorDebugger);
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
        resolveProcessorRepresentation(muleContext.getConfiguration().getId(), notification.getProcessorPath(),
                                       notification.getProcessor());
    if (notification.getSource().getProcessorsTrace() instanceof DefaultProcessorsTrace) {
      ((DefaultProcessorsTrace) notification.getSource().getProcessorsTrace())
          .addExecutedProcessors(resolveProcessorRepresentation);
    }
    if (notification.getSource().getFlowCallStack() instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) notification.getSource().getFlowCallStack())
          .setCurrentProcessorPath(resolveProcessorRepresentation);
    }
  }

  /**
   * Callback method for when a flow or sub-flow called from a {@code flow-ref} component has been completed.
   * 
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onPipelineNotificationComplete(PipelineMessageNotification notification) {
    onFlowComplete((MuleEvent) notification.getSource());
  }

  /**
   * Callback method for when a flow or sub-flow is about to be called from a {@code flow-ref}.
   * 
   * @param notification the notification that contains the event and the processor that is about to be invoked.
   */
  public void onPipelineNotificationStart(PipelineMessageNotification notification) {
    onFlowStart((MuleEvent) notification.getSource(), notification.getResourceIdentifier());
  }

  @Override
  public void onFlowStart(MuleEvent muleEvent, String flowName) {
    if (muleEvent.getFlowCallStack() instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) muleEvent.getFlowCallStack()).push(new FlowStackElement(flowName, null));
    }
  }

  @Override
  public void onFlowComplete(MuleEvent muleEvent) {
    if (muleEvent.getFlowCallStack() instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) muleEvent.getFlowCallStack()).pop();
    }
  }

  @Override
  public Map<String, Object> getContextInfo(MuleEvent muleEvent, MessageProcessor lastProcessed) {
    if (DefaultMuleConfiguration.isFlowTrace()) {
      return Collections.<String, Object>singletonMap(FLOW_STACK_INFO_KEY, muleEvent.getFlowCallStack().toString());
    } else {
      return Collections.<String, Object>emptyMap();
    }
  }
}
