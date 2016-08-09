/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class JSR250LifecycleTrackerComponent implements Startable, Stoppable, MuleContextAware, FlowConstructAware, Callable {

  private final List<String> tracker = new ArrayList<String>();

  public List<String> getTracker() {
    return tracker;
  }

  public void setProperty(final String value) {
    tracker.add("setProperty");
  }

  public void setMuleContext(final MuleContext context) {
    tracker.add("setMuleContext");
  }

  @PostConstruct
  public void initialise() {
    tracker.add("jsr250 initialise");
  }

  @PreDestroy
  public void dispose() {
    tracker.add("jsr250 dispose");
  }

  public void start() throws MuleException {
    tracker.add("start");
  }

  public void stop() throws MuleException {
    tracker.add("stop");
  }

  public void setFlowConstruct(final FlowConstruct flowConstruct) {
    getTracker().add("setFlowConstruct");
  }

  public Object onCall(final MuleEventContext eventContext) throws Exception {
    // dirty trick to get the component instance that was used for the
    // request
    return this;
  }
}
