/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
