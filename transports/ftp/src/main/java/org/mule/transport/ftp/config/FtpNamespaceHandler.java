/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.ftp.FtpConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><ftp:connector></code> elements.
 */
public class FtpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(FtpConnector.FTP, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(FtpConnector.class, FtpConnector.FTP);
        
        registerBeanDefinitionParser("custom-filename-parser", new ChildDefinitionParser("filenameParser", null, FilenameParser.class));
        registerBeanDefinitionParser("expression-filename-parser", new ChildDefinitionParser("filenameParser", ExpressionFilenameParser.class));
    }
}
