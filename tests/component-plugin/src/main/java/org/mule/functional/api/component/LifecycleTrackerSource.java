/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.core.lifecyle.AbstractLifecycleTracker;

import java.util.ArrayList;
import java.util.List;


public class LifecycleTrackerSource extends AbstractLifecycleTracker implements MessageSource {

  private static List<LifecycleTrackerSource> sources = new ArrayList<>();

  private Processor listener;

  public LifecycleTrackerSource() {
    sources.add(this);
  }

  public static List<LifecycleTrackerSource> getSources() {
    return sources;
  }

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

}
