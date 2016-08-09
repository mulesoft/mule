/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.config;

import org.mule.compatibility.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpsConnector;
import org.mule.compatibility.transport.http.HttpsPollingConnector;
import org.mule.compatibility.transport.http.components.StaticResourceMessageProcessor;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><https:connector></code> elements.
 */
public class HttpsNamespaceHandler extends HttpNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(HttpsConnector.HTTPS, URIBuilder.SOCKET_ATTRIBUTES)
        .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE).addAlias("method", HttpConnector.HTTP_METHOD_PROPERTY);

    registerDeprecatedConnectorDefinitionParser(HttpsConnector.class);
    registerDeprecatedBeanDefinitionParser("polling-connector",
                                           new MuleOrphanDefinitionParser(HttpsPollingConnector.class, true));

    registerDeprecatedBeanDefinitionParser("tls-key-store", new KeyStoreDefinitionParser());
    registerDeprecatedBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
    registerDeprecatedBeanDefinitionParser("tls-server", new TrustStoreDefinitionParser());
    registerDeprecatedBeanDefinitionParser("tls-protocol-handler", new ProtocolHandlerDefinitionParser());

    registerDeprecatedMuleBeanDefinitionParser("static-resource-handler",
                                               new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class));
  }

  @Override
  protected MuleDefinitionParserConfiguration registerStandardTransportEndpoints(String protocol, String[] requiredAttributes) {
    return new TransportRegisteredMdps(protocol, AddressedEndpointDefinitionParser.PROTOCOL, requiredAttributes);
  }

  @Override
  protected MuleDefinitionParserConfiguration registerMetaTransportEndpoints(String protocol) {
    return new TransportRegisteredMdps(protocol, AddressedEndpointDefinitionParser.META, new String[] {});
  }

  @Override
  protected Class getInboundEndpointFactoryBeanClass() {
    return InboundEndpointFactoryBean.class;
  }

  @Override
  protected Class getOutboundEndpointFactoryBeanClass() {
    return OutboundEndpointFactoryBean.class;
  }

  @Override
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
