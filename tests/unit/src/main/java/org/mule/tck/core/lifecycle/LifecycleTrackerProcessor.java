/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.lifecycle;

import static org.mockito.Mockito.mock;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.runtime.core.api.processor.Processor;

import javax.inject.Inject;

public class LifecycleTrackerProcessor extends AbstractLifecycleTracker implements Processor {

  public static String LIFECYCLE_TRACKER_PROCESSOR_PROPERTY = "lifecycle";
  public static String FLOW_CONSRUCT_PROPERTY = "flowConstruct";

  @Inject
  private ConfigurationComponentLocator componentLocator;

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
  public Event process(Event event) throws MuleException {
    event = Event.builder(event)
        .addVariable(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY, getTracker().toString())
        .addVariable(FLOW_CONSRUCT_PROPERTY, FlowConstruct.getFromAnnotatedObject(componentLocator, this)).build();
    return event;
  }
}
