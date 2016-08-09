/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.factories;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.registry.LegacyServiceType;
import org.mule.compatibility.core.endpoint.AbstractEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.service.TransportServiceDescriptor;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

/**
 * Spring FactoryBean used to create concrete instances of outbound endpoints
 */
public class OutboundEndpointFactoryBean extends AbstractEndpointFactoryBean {

  public OutboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException {
    super(global);
  }

  public OutboundEndpointFactoryBean() {
    super();
  }

  @Override
  public Class getObjectType() {
    return OutboundEndpoint.class;
  }

  @Override
  public Object doGetObject() throws Exception {
    // If this is a meta endpoint, then we can wrap it using the meta endpoint builder from the TransportServiceDescriptor
    String scheme = getEndpointBuilder().getEndpoint().getFullScheme();
    TransportServiceDescriptor tsd =
        (TransportServiceDescriptor) lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT, scheme,
                                                             null);
    EndpointBuilder endpointBuilder = tsd.createEndpointBuilder(this, muleContext);

    OutboundEndpoint outboundEndpoint = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    if (outboundEndpoint instanceof AbstractEndpoint) {
      AbstractEndpoint.class.cast(outboundEndpoint).setAnnotations(getAnnotations());
    }
    return outboundEndpoint;
  }



  @Override
  public void setRedeliveryPolicy(AbstractRedeliveryPolicy redeliveryPolicy) {
    throw new IllegalStateException("A redelivery policy cannot be specified for an outbound endpoint.");
  }

}
