/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle.processor;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.isStopped;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;

public class ProcessIfStartedMessageProcessor extends AbstractFilteringMessageProcessor {

  protected Startable startable;
  protected LifecycleState lifecycleState;

  public ProcessIfStartedMessageProcessor(Startable startable, LifecycleState lifecycleState) {
    this.startable = startable;
    this.lifecycleState = lifecycleState;
  }

  @Override
  protected boolean accept(Event event, Event.Builder builder) {
    return lifecycleState.isStarted();
  }

  @Override
  public boolean isThrowOnUnaccepted() {
    return true;
  }

  @Override
  protected MuleException filterUnacceptedException(Event event) {
    return new LifecycleException(isStopped(getStartableName(startable)), event.getMessage());
  }

  protected String getStartableName(Startable startableObject) {
    if (startableObject instanceof NameableObject) {
      return ((NameableObject) startableObject).getName();
    } else {
      return startableObject.toString();
    }
  }

}
