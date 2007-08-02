/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.routing.filters.xml.IsXmlFilter;
import org.mule.routing.filters.xml.JXPathFilter;

/**
 * Handles all filters in XML config module (separate namespaces for separate component
 * types helps keep schema restricted)
 */
public class XmlFilterNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jxpath", new ChildDefinitionParser("filter", JXPathFilter.class));
        registerBeanDefinitionParser("is-xml", new ChildDefinitionParser("filter", IsXmlFilter.class));
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));
    }
    
}
