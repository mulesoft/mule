/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;

import java.util.List;

public class DynamicFlowFactory {

  /**
   * Creates a programmatic flow
   *
   * @param muleContext the context of the application
   * @param flowName the flow name
   * @param messageProcessors the flow message processors
   * @return a new flow
   * @throws MuleException if there was a failure registering the flow in mule.
   */
  public static Flow createDynamicFlow(final MuleContext muleContext, String flowName, List<Processor> messageProcessors)
      throws MuleException {
    final Flow flow = new Flow(flowName, muleContext);
    flow.setMessageProcessors(messageProcessors);
    muleContext.getRegistry().registerFlowConstruct(flow);
    return flow;
  }
}
