/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.email.GmailSmtpConnector;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.SmtpConnector;

/**
 * Namespace handler for the <smtp:xxx> namespace
 */
public class SmtpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(SmtpConnector.SMTP, URIBuilder.HOST_ATTRIBUTES)
                .addAlias("to", MailProperties.TO_ADDRESSES_PROPERTY)
                .addAlias("from", MailProperties.FROM_ADDRESS_PROPERTY)
                .addAlias("cc", MailProperties.CC_ADDRESSES_PROPERTY)
                .addAlias("bcc", MailProperties.BCC_ADDRESSES_PROPERTY)
                .addAlias("from", MailProperties.FROM_ADDRESS_PROPERTY)
                .addAlias("replyTo", MailProperties.REPLY_TO_ADDRESSES_PROPERTY);
        registerConnectorDefinitionParser(SmtpConnector.class);
        registerBeanDefinitionParser("header", new ChildMapEntryDefinitionParser("customHeaders", "key", "value"));
        registerBeanDefinitionParser("gmail-connector", new MuleOrphanDefinitionParser(GmailSmtpConnector.class, true));
    }
}
