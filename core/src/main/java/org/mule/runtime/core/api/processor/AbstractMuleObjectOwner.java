/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

/**
 * An object that owns Mule objects and delegates startup/shutdown events to them.
 */
public abstract class AbstractMuleObjectOwner<T> extends AbstractComponent
    implements Lifecycle, MuleContextAware {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  // TODO MULE-10332: Review MuleContextAware vs @Inject usage
  @Inject
  protected MuleContext muleContext;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    setMuleContextIfNeeded(getOwnedObjects(), muleContext);
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    // TODO TMULE-10764 This shouldn't happen here.
    setMuleContext(muleContext);
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
