/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.config;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.module.cxf.CxfInboundMessageProcessor;
import org.mule.compatibility.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;

public class DecoupledEndpointBuilder {

  public static void build(MuleContext muleContext, String decoupledEndpoint, CxfOutboundMessageProcessor processor, Bus bus) {
    if (decoupledEndpoint != null) {
      processor.setDecoupledEndpoint(decoupledEndpoint);

      CxfInboundMessageProcessor cxfInboundMP = new CxfInboundMessageProcessor();
      cxfInboundMP.setMuleContext(muleContext);
      cxfInboundMP.setBus(bus);

      List<Processor> mps = new ArrayList<Processor>();
      mps.add(cxfInboundMP);

      try {
        EndpointBuilder ep = getEndpointFactory(muleContext).getEndpointBuilder(decoupledEndpoint);

        Flow flow = new Flow("decoupled-" + ep.toString(), muleContext);
        flow.setMessageProcessors(mps);
        flow.setMessageSource(ep.buildInboundEndpoint());
        muleContext.getRegistry().registerObject(flow.getName(), flow);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }

  }

  private static EndpointFactory getEndpointFactory(MuleContext muleContext) {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
