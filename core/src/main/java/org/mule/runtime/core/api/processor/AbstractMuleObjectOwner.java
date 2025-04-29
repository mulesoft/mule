/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * An object that owns Mule objects and delegates startup/shutdown events to them.
 */
public abstract class AbstractMuleObjectOwner<T> extends AbstractComponent
    implements Lifecycle {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  // TODO MULE-10332: Review MuleContextAware vs @Inject usage
  protected MuleContext muleContext;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    setMuleContextIfNeeded(getOwnedObjects(), muleContext);
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(getOwnedObjects(), muleContext.getInjector());
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
