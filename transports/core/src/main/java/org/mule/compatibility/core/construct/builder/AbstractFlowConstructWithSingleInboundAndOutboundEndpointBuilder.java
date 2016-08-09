/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.construct.builder;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.construct.AbstractFlowConstruct;
import org.mule.runtime.core.construct.builder.AbstractFlowConstructBuilder;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
@SuppressWarnings("unchecked")
public abstract class AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<T extends AbstractFlowConstructBuilder<?, ?>, F extends AbstractFlowConstruct>
    extends AbstractFlowConstructWithSingleInboundEndpointBuilder<T, F> {

  private OutboundEndpoint outboundEndpoint;
  private EndpointBuilder outboundEndpointBuilder;
  private String outboundAddress;

  public T outboundEndpoint(OutboundEndpoint outboundEndpoint) {
    this.outboundEndpoint = outboundEndpoint;
    return (T) this;
  }

  public T outboundEndpoint(EndpointBuilder outboundEndpointBuilder) {
    this.outboundEndpointBuilder = outboundEndpointBuilder;
    return (T) this;
  }

  public T outboundAddress(String outboundAddress) {
    this.outboundAddress = outboundAddress;
    return (T) this;
  }

  protected OutboundEndpoint getOrBuildOutboundEndpoint(MuleContext muleContext) throws MuleException {
    if (outboundEndpoint != null) {
      return outboundEndpoint;
    }

    if (outboundEndpointBuilder == null) {
      outboundEndpointBuilder = getEndpointFactory(muleContext.getRegistry()).getEndpointBuilder(outboundAddress);
    }

    outboundEndpointBuilder.setExchangePattern(getOutboundMessageExchangePattern());

    doConfigureOutboundEndpointBuilder(muleContext, outboundEndpointBuilder);

    return outboundEndpointBuilder.buildOutboundEndpoint();
  }

  protected abstract MessageExchangePattern getOutboundMessageExchangePattern();

  protected void doConfigureOutboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder) {
    // template method
  }

  public EndpointFactory getEndpointFactory(MuleRegistry registry) {
    return (EndpointFactory) registry.lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
