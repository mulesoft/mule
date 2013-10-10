/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.email.ImapsConnector;

/**
 * Registers a Bean Definition Parser for handling <code><imaps:connector></code> elements.
 *
 */
public class ImapsNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(ImapsConnector.IMAPS, URIBuilder.USERHOST_ATTRIBUTES);

        MuleDefinitionParserConfiguration parser = registerConnectorDefinitionParser(ImapsConnector.class);
        parser.addMapping("defaultProcessMessageAction", EmailNamespaceHandler.DEFAULT_PROCESS_MESSAGE_ACTION);

        registerBeanDefinitionParser("tls-trust-store", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
    }
}
