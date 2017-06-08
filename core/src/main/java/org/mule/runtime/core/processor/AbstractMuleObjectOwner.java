/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setFlowConstructIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that owns Mule objects and delegates startup/shutdown events to them.
 */
public abstract class AbstractMuleObjectOwner<T> extends AbstractAnnotatedObject
    implements Lifecycle, MuleContextAware, FlowConstructAware, MessagingExceptionHandlerAware {

  // TODO MULE-10332: Review MuleContextAware vs @Inject usage
  @Inject
  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;
  protected MessagingExceptionHandler messagingExceptionHandler;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    setMuleContextIfNeeded(getOwnedObjects(), muleContext);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
    setFlowConstructIfNeeded(getOwnedObjects(), flowConstruct);
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

  @Override
  public void initialise() throws InitialisationException {
    // TODO TMULE-10764 This shouldn't happen here.
    setMuleContext(muleContext);
    setFlowConstruct(flowConstruct);
    for (T object : getOwnedObjects()) {
      if (messagingExceptionHandler != null && object instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) object).setMessagingExceptionHandler(messagingExceptionHandler);
      }
    }
    initialiseIfNeeded(getOwnedObjects(), true, muleContext);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(getOwnedObjects(), logger);
  }


  @Override
  public void start() throws MuleException {
    startIfNeeded(getOwnedObjects());
  }


  @Override
  public void stop() throws MuleException {
    stopIfNeeded(getOwnedObjects());
  }

  protected abstract List<T> getOwnedObjects();

}
