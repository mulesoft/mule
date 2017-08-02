/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
