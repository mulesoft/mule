/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.assembly.configuration.ListValueMap;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.providers.soap.ServiceProxy;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;axis:connector&gt;</code> elements.
 */
public class AxisNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(AxisConnector.AXIS);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(AxisConnector.class, true));
        registerBeanDefinitionParser("bean-type", new ChildListEntryDefinitionParser("beanTypes"));
        registerBeanDefinitionParser("supported-scheme", new ChildListEntryDefinitionParser("supportedSchemes"));
    }

}


