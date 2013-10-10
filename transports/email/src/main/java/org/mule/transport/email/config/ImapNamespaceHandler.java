/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.email.ImapConnector;

/**
 * Registers a Bean Definition Parser for handling <code><imap:connector></code> elements.
 *
 */
public class ImapNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(ImapConnector.IMAP, URIBuilder.USERHOST_ATTRIBUTES);

        MuleDefinitionParserConfiguration parser = registerConnectorDefinitionParser(ImapConnector.class);
        parser.addMapping("defaultProcessMessageAction", EmailNamespaceHandler.DEFAULT_PROCESS_MESSAGE_ACTION);
    }
}
