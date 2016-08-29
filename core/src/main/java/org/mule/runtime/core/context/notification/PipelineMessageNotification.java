/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;

/**
 * <code>PipelineMessageNotification</code> is fired at key steps in the processing of {@link Pipeline}
 */
public class PipelineMessageNotification extends ServerNotification implements BlockingServerEvent {

  private static final long serialVersionUID = 6065691696506216248L;

  // Fired when processing of pipeline starts
  public static final int PROCESS_START = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 1;
  // Fired when pipeline processing reaches the end before returning
  public static final int PROCESS_END = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 2;
  // Fired when pipeline processing returns after processing request and response message
  public static final int PROCESS_COMPLETE = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 4;

  static {
    registerAction("pipeline process start", PROCESS_START);
    registerAction("pipeline request message processing end", PROCESS_END);
    registerAction("pipeline process complete", PROCESS_COMPLETE);
  }

  protected MessagingException exception;

  public PipelineMessageNotification(Pipeline pipeline, MuleEvent event, int action) {
    super(event, action, pipeline.getName());
  }

  public PipelineMessageNotification(Pipeline pipeline, MuleEvent event, int action, MessagingException exception) {
    this(pipeline, event, action);
    this.exception = exception;
  }

  public MessagingException getException() {
    return exception;
  }

}
