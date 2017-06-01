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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ObjectUtils;

public class FlowConstructStatisticsMessageProcessor extends AbstractAnnotatedObject
    implements Processor, FlowConstructAware, InternalMessageProcessor {

  protected FlowConstruct flowConstruct;

  @Override
  public Event process(Event event) throws MuleException {
    if (flowConstruct.getStatistics().isEnabled()) {
      flowConstruct.getStatistics().incReceivedEvents();
    }

    return event;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
