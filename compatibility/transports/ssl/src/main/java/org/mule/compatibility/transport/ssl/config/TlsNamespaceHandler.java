/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl.config;

import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.ssl.TlsConnector;
import org.mule.runtime.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.runtime.module.tls.internal.config.KeyStoreParentContextDefinitionParser;
import org.mule.runtime.module.tls.internal.config.TlsContextDefinitionParser;
import org.mule.runtime.module.tls.internal.config.TrustStoreTlsContextDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><tls:connector></code> elements.
 */
public class TlsNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(TlsConnector.TLS, URIBuilder.SOCKET_ATTRIBUTES);
    registerConnectorDefinitionParser(TlsConnector.class);
    registerBeanDefinitionParser("key-store", new KeyStoreParentContextDefinitionParser());
    registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
    registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
    registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());
    registerBeanDefinitionParser("context", new TlsContextDefinitionParser());
    registerBeanDefinitionParser("trust-store", new TrustStoreTlsContextDefinitionParser());
  }

}
