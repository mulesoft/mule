/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.config;

import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.file.ExpressionFilenameParser;
import org.mule.compatibility.transport.file.FileConnector;
import org.mule.compatibility.transport.file.FilenameParser;
import org.mule.compatibility.transport.file.filters.FilenameRegexFilter;
import org.mule.compatibility.transport.file.filters.FilenameWildcardFilter;
import org.mule.compatibility.transport.file.transformers.FileToByteArray;
import org.mule.compatibility.transport.file.transformers.FileToString;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><file:connector></code> elements.
 *
 */
public class FileNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(FileConnector.FILE, URIBuilder.PATH_ATTRIBUTES);
    registerConnectorDefinitionParser(FileConnector.class);

    registerBeanDefinitionParser("custom-filename-parser",
                                 new ChildDefinitionParser("filenameParser", null, FilenameParser.class));
    registerBeanDefinitionParser("expression-filename-parser",
                                 new ChildDefinitionParser("filenameParser", ExpressionFilenameParser.class));

    registerBeanDefinitionParser("file-to-byte-array-transformer",
                                 new TransformerMessageProcessorDefinitionParser(FileToByteArray.class));
    registerBeanDefinitionParser("file-to-string-transformer",
                                 new TransformerMessageProcessorDefinitionParser(FileToString.class));

    registerBeanDefinitionParser("filename-wildcard-filter", new FilterDefinitionParser(FilenameWildcardFilter.class));
    registerBeanDefinitionParser("filename-regex-filter", new FilterDefinitionParser(FilenameRegexFilter.class));
  }

}
