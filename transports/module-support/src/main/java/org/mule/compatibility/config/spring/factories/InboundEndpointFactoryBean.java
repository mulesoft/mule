/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.factories;

import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.AbstractEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.config.i18n.MessageFactory;

/**
 * Spring FactoryBean used to create concrete instances of inbound endpoints
 */
public class InboundEndpointFactoryBean extends AbstractEndpointFactoryBean {

  public InboundEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException {
    super(global);
  }

  public InboundEndpointFactoryBean() {
    super();
  }

  @Override
  public Class<?> getObjectType() {
    return InboundEndpoint.class;
  }

  @Override
  public Object doGetObject() throws Exception {
    EndpointFactory ef = getEndpointFactory();
    if (ef != null) {
      InboundEndpoint inboundEndpoint = ef.getInboundEndpoint(this);
      if (inboundEndpoint instanceof AbstractEndpoint) {
        AbstractEndpoint.class.cast(inboundEndpoint).setAnnotations(getAnnotations());
      }
      return inboundEndpoint;
    } else {
      throw new ConfigurationException(MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
    }
  }

}
