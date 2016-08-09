/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;

import java.util.List;

/**
 * An object that owns Mule objects and delegates startup/shutdown events to them.
 */
public abstract class AbstractMuleObjectOwner<T>
    implements Lifecycle, MuleContextAware, FlowConstructAware, MessagingExceptionHandlerAware {

  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;
  protected MessagingExceptionHandler messagingExceptionHandler;

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    if (this.messagingExceptionHandler == null) {
      this.messagingExceptionHandler = messagingExceptionHandler;
    }
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  public void initialise() throws InitialisationException {
    for (T object : getOwnedObjects()) {
      if (object instanceof MuleContextAware) {
        ((MuleContextAware) object).setMuleContext(muleContext);
      }
      if (object instanceof FlowConstructAware) {
        ((FlowConstructAware) object).setFlowConstruct(flowConstruct);
      }
      if (messagingExceptionHandler != null && object instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) object).setMessagingExceptionHandler(messagingExceptionHandler);
      }
      if (object instanceof Initialisable) {
        ((Initialisable) object).initialise();
      }
    }
  }

  public void dispose() {
    for (T processor : getOwnedObjects()) {

      if (processor instanceof Disposable) {
        ((Disposable) processor).dispose();
      }
    }
  }


  public void start() throws MuleException {

    for (T processor : getOwnedObjects()) {
      if (processor instanceof Startable) {
        ((Startable) processor).start();
      }
    }
  }


  public void stop() throws MuleException {

    for (T processor : getOwnedObjects()) {
      if (processor instanceof Stoppable) {
        ((Stoppable) processor).stop();
      }

    }
  }

  protected MessagingExceptionHandler getMessagingExceptionHandler() {
    return messagingExceptionHandler;
  }

  protected abstract List<T> getOwnedObjects();


}
