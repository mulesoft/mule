/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.sftp.SftpConnector;

/**
 * Registers a Bean Definition Parser for handling <code><sftp:connector></code>
 * elements.
 */
public class SftpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new OrphanDefinitionParser(SftpConnector.class, true));
        registerBeanDefinitionParser("expression-filename-parser", new ChildDefinitionParser(
            "filenameParser", ExpressionFilenameParser.class));
        registerStandardTransportEndpoints("sftp", URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(SftpConnector.class);
    }
}
