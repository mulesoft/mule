/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.mockito.Mockito.mock;
import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.management.stats.ComponentStatistics;
import org.mule.test.core.lifecycle.AbstractLifecycleTracker;

public class LifecycleTrackerProcessor extends AbstractLifecycleTracker implements FlowConstructAware, Processor {

  public static String LIFECYCLE_TRACKER_PROCESSOR_PROPERTY = "lifecycle";
  public static String FLOW_CONSRUCT_PROPERTY = "flowConstruct";

  private FlowConstruct flowConstruct;

  public void springInitialize() {
    getTracker().add("springInitialize");
  }

  public void springDestroy() {
    getTracker().add("springDestroy");
  }

  @Override
  public void setFlowConstruct(final FlowConstruct flowConstruct) {
    if (this.flowConstruct != flowConstruct) {
      getTracker().add("setService");
      this.flowConstruct = flowConstruct;
    }
  }

  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  public ComponentStatistics getStatistics() {
    return mock(ComponentStatistics.class);
  }

  @Override
  public Event process(Event event) throws MuleException {
    event = Event.builder(event)
        .message(new TestLegacyMessageBuilder(event.getMessage())
            .addOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY, getTracker().toString()).build())
        .addVariable(FLOW_CONSRUCT_PROPERTY, flowConstruct).build();
    return event;
  }
}
