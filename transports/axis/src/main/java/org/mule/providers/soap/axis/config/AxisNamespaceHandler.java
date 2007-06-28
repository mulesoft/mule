/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.providers.soap.axis.AxisConnector;

import org.springframework.beans.factory.xml.UtilNamespaceHandler;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;axis:connector&gt;</code> elements.
 */
public class AxisNamespaceHandler extends UtilNamespaceHandler // NamespaceHandlerSupport
{
    public void init()
    {
        this.registerBeanDefinitionParser("connector", new OrphanDefinitionParser(AxisConnector.class, true));
        this.registerBeanDefinitionParser("bean-type", new ChildListEntryDefinitionParser("beanTypes"));
        this.registerBeanDefinitionParser("supported-scheme", new ChildListEntryDefinitionParser("supportedSchemes"));
    }
}


