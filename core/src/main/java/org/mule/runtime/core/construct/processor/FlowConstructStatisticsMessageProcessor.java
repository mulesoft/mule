/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct.processor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;

public class FlowConstructStatisticsMessageProcessor implements Processor, FlowConstructAware, InternalMessageProcessor {

  protected FlowConstruct flowConstruct;

  public Event process(Event event) throws MuleException {
    if (flowConstruct.getStatistics().isEnabled()) {
      if (event.getExchangePattern().hasResponse()) {
        flowConstruct.getStatistics().incReceivedEventSync();
      } else {
        flowConstruct.getStatistics().incReceivedEventASync();
      }
    }

    return event;
  }

  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
