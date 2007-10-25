/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.file.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.providers.file.FileConnector;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.transformers.FileToByteArray;
import org.mule.providers.file.transformers.FileToString;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class FileNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(FileConnector.class, true));
        registerBeanDefinitionParser("filename-parser",
                    new ChildDefinitionParser("filenameParser", null, FilenameParser.class));
        registerBeanDefinitionParser("transformer-file-to-byte-array", new MuleOrphanDefinitionParser(FileToByteArray.class, false));
        registerBeanDefinitionParser("transformer-file-to-string", new MuleOrphanDefinitionParser(FileToString.class, false));
    }
}