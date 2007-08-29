/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jdbc.config;

import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.providers.jdbc.JdbcConnector;
import org.mule.providers.jdbc.NowPropertyExtractor;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "jdbc" namespace. */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleChildDefinitionParser(JdbcConnector.class, true));
        registerBeanDefinitionParser("dataSource", new ObjectFactoryDefinitionParser("dataSourceFactory"));
        registerBeanDefinitionParser("queries", new ChildMapDefinitionParser("queries"));
        registerBeanDefinitionParser("extractors", new ParentDefinitionParser());
        registerBeanDefinitionParser("now-property-extractor", new ChildDefinitionParser("propertyExtractor", NowPropertyExtractor.class));
    }

}
