/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.lifecycle;

import static org.mockito.Mockito.mock;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.core.lifecyle.AbstractLifecycleTracker;

import javax.inject.Inject;

public class LifecycleTrackerProcessor extends AbstractLifecycleTracker implements Processor {

  public static String LIFECYCLE_TRACKER_PROCESSOR_PROPERTY = "lifecycle";
  public static String FLOW_CONSRUCT_PROPERTY = "flowConstruct";

  @Inject
  private Registry registry;

  public void springInitialize() {
    getTracker().add("springInitialize");
  }

  public void springDestroy() {
    getTracker().add("springDestroy");
  }

  public ComponentStatistics getStatistics() {
    return mock(ComponentStatistics.class);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    event = CoreEvent.builder(event)
        .addVariable(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY, getTracker().toString())
        .addVariable(FLOW_CONSRUCT_PROPERTY, registry.lookupByName(getRootContainerName()).orElse(null)).build();
    return event;
  }
}
