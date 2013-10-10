/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.SmtpsConnector;

/**
 * Namespace handler for the <smtps:xxx> namespace
 */
public class SmtpsNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(SmtpsConnector.SMTPS, URIBuilder.HOST_ATTRIBUTES)
                .addAlias("to", MailProperties.TO_ADDRESSES_PROPERTY)
                .addAlias("from", MailProperties.FROM_ADDRESS_PROPERTY)
                .addAlias("cc", MailProperties.CC_ADDRESSES_PROPERTY)
                .addAlias("bcc", MailProperties.BCC_ADDRESSES_PROPERTY)
                .addAlias("from", MailProperties.FROM_ADDRESS_PROPERTY)
                .addAlias("replyTo", MailProperties.REPLY_TO_ADDRESSES_PROPERTY);
        registerConnectorDefinitionParser(SmtpsConnector.class);
        registerBeanDefinitionParser("header", new ChildMapEntryDefinitionParser("customHeaders", "key", "value"));
        registerBeanDefinitionParser("tls-trust-store", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
    }
}
