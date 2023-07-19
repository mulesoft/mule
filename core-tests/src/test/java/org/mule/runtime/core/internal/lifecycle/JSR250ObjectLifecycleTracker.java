/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class JSR250ObjectLifecycleTracker implements MuleContextAware {

  private final List<String> tracker = new ArrayList<String>();

  public List<String> getTracker() {
    return tracker;
  }

  public void setMuleContext(MuleContext context) {
    tracker.add("setMuleContext");
  }

  @PostConstruct
  public void init() {
    tracker.add("initialise");
  }

  @PreDestroy
  public void dispose() {
    tracker.add("dispose");
  }
}
