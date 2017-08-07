/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct.processor;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ObjectUtils;

public class FlowConstructStatisticsMessageProcessor extends AbstractAnnotatedObject
    implements Processor, InternalMessageProcessor {

  protected FlowConstructStatistics flowConstructStatistics;

  public FlowConstructStatisticsMessageProcessor(FlowConstructStatistics flowConstructStatistics) {
    this.flowConstructStatistics = flowConstructStatistics;
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (flowConstructStatistics.isEnabled()) {
      flowConstructStatistics.incReceivedEvents();
    }

    return event;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
