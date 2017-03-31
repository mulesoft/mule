/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.filter;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.routing.filter.Filter;

import java.util.ArrayList;
import java.util.List;

public class LifecycleTrackerFilter implements Filter, Lifecycle, MuleContextAware {

  private final List<String> tracker = new ArrayList<String>();

  public List<String> getTracker() {
    return tracker;
  }

  public void setProperty(final String value) {
    tracker.add("setProperty");
  }

  @Override
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

  @Override
  public boolean accept(Message message, Event.Builder builder) {
    // TODO Auto-generated method stub
    return false;
  }

}

