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

import org.mule.config.spring.parsers.collection.MapEntryDefinitionParser;
import org.mule.config.spring.parsers.general.SimpleChildDefinitionParser;
import org.mule.routing.filters.xml.JXPathFilter;

/**
 * Handles all configuration elements in the Mule Xml module.
 */
public class XmlNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jxpath-filter", new SimpleChildDefinitionParser("filter", JXPathFilter.class));
        registerBeanDefinitionParser("namespace", new MapEntryDefinitionParser("namespaces", "prefix", "uri"));

    }
}
