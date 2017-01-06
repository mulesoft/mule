/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.handlers;

import org.mule.compatibility.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;

public abstract class AbstractMuleTransportsNamespaceHandler extends AbstractMuleNamespaceHandler {

  @Override
  protected MuleDefinitionParserConfiguration registerStandardTransportEndpoints(String protocol, String[] requiredAttributes) {
    return new TransportRegisteredMdps(protocol, AddressedEndpointDefinitionParser.PROTOCOL, requiredAttributes);
  }

  @Override
  protected MuleDefinitionParserConfiguration registerMetaTransportEndpoints(String protocol) {
    return new TransportRegisteredMdps(protocol, AddressedEndpointDefinitionParser.META, new String[] {});
  }

  protected Class getInboundEndpointFactoryBeanClass() {
    return InboundEndpointFactoryBean.class;
  }

  protected Class getOutboundEndpointFactoryBeanClass() {
    return OutboundEndpointFactoryBean.class;
  }

  protected Class getGlobalEndpointBuilderBeanClass() {
    return EndpointURIEndpointBuilder.class;
  }

  protected class TransportRegisteredMdps extends RegisteredMdps {

    public TransportRegisteredMdps(String protocol, boolean isMeta, String[] requiredAttributes) {
      registerBeanDefinitionParser("endpoint",
                                   add(new TransportGlobalEndpointDefinitionParser(protocol, isMeta,
                                                                                   getGlobalEndpointBuilderBeanClass(),
                                                                                   requiredAttributes, new String[] {})));
      registerBeanDefinitionParser("inbound-endpoint",
                                   add(new TransportEndpointDefinitionParser(protocol, isMeta,
                                                                             getInboundEndpointFactoryBeanClass(),
                                                                             requiredAttributes, new String[] {})));
      registerBeanDefinitionParser("outbound-endpoint",
                                   add(new TransportEndpointDefinitionParser(protocol, isMeta,
                                                                             getOutboundEndpointFactoryBeanClass(),
                                                                             requiredAttributes, new String[] {})));
    }
  }


}
