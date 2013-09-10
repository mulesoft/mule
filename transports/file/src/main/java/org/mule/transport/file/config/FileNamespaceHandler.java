/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.file.FileConnector;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.file.filters.FilenameRegexFilter;
import org.mule.transport.file.filters.FilenameWildcardFilter;
import org.mule.transport.file.transformers.FileToByteArray;
import org.mule.transport.file.transformers.FileToString;

/**
 * Reigsters a Bean Definition Parser for handling <code><file:connector></code> elements.
 *
 */
public class FileNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerStandardTransportEndpoints(FileConnector.FILE, URIBuilder.PATH_ATTRIBUTES);
        registerConnectorDefinitionParser(FileConnector.class);

        registerBeanDefinitionParser("custom-filename-parser", new ChildDefinitionParser("filenameParser", null, FilenameParser.class));
        registerBeanDefinitionParser("expression-filename-parser", new ChildDefinitionParser("filenameParser", ExpressionFilenameParser.class));

        registerBeanDefinitionParser("file-to-byte-array-transformer", new MessageProcessorDefinitionParser(FileToByteArray.class));
        registerBeanDefinitionParser("file-to-string-transformer", new MessageProcessorDefinitionParser(FileToString.class));
        
        registerBeanDefinitionParser("filename-wildcard-filter", new FilterDefinitionParser(FilenameWildcardFilter.class));
        registerBeanDefinitionParser("filename-regex-filter", new FilterDefinitionParser(FilenameRegexFilter.class));
    }

}
