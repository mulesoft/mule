/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncWorkListener implements WorkListener {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  protected MessageProcessor target;

  public AsyncWorkListener(MessageProcessor target) {
    this.target = target;
  }

  public void workAccepted(WorkEvent event) {
    this.handleWorkException(event, "workAccepted");
  }

  public void workRejected(WorkEvent event) {
    this.handleWorkException(event, "workRejected");
  }

  public void workStarted(WorkEvent event) {
    this.handleWorkException(event, "workStarted");
  }

  public void workCompleted(WorkEvent event) {
    this.handleWorkException(event, "workCompleted");
  }

  protected void handleWorkException(WorkEvent event, String type) {
    if (event == null) {
      return;
    }

    Throwable e = event.getException();

    if (e == null) {
      return;
    }

    if (e.getCause() != null) {
      e = e.getCause();
    }

    logger.error("Work caused exception on '" + type + "'. Work being executed was: " + event.getWork().toString());
    throw new MuleRuntimeException(CoreMessages.errorInvokingMessageProcessorAsynchronously(target), e);
  }

}
