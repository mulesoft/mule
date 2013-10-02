/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
