/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
