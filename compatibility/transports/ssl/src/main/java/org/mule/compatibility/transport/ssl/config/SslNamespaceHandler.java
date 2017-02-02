/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl.config;

import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.ssl.SslConnector;
import org.mule.runtime.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><ssl:connector></code> elements.
 */
public class SslNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(SslConnector.SSL, URIBuilder.SOCKET_ATTRIBUTES);
    registerConnectorDefinitionParser(SslConnector.class);
    registerBeanDefinitionParser("key-store", new KeyStoreDefinitionParser());
    registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
    registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
    registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());
  }

}
