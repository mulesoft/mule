/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.email.Pop3sConnector;

/**
 * Registers a Bean Definition Parser for handling <code><pop3s:connector></code> elements.
 *
 */
public class Pop3sNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(Pop3sConnector.POP3S, URIBuilder.USERHOST_ATTRIBUTES);

        MuleDefinitionParserConfiguration parser = registerConnectorDefinitionParser(Pop3sConnector.class);
        parser.addMapping("defaultProcessMessageAction", EmailNamespaceHandler.DEFAULT_PROCESS_MESSAGE_ACTION);

        registerBeanDefinitionParser("tls-trust-store", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
    }
}
