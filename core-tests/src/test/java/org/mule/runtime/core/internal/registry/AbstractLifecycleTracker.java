/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

public abstract class AbstractLifecycleTracker implements Lifecycle, MuleContextAware {

  private final List<String> tracker = new ArrayList<>();

  public List<String> getTracker() {
    return tracker;
  }

  public void setProperty(final String value) {
    tracker.add("setProperty");
  }

  @Inject
  public void setMuleContext(final MuleContext context) {
    tracker.add("setMuleContext");
  }

  @Override
  public void initialise() throws InitialisationException {
    tracker.add("initialise");
  }

  @Override
  public void start() throws MuleException {
    tracker.add("start");
  }

  @Override
  public void stop() throws MuleException {
    tracker.add("stop");
  }

  @Override
  public void dispose() {
    tracker.add("dispose");
  }

}
