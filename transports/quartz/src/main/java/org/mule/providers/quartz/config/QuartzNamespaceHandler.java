/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.quartz.config;

import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.providers.quartz.QuartzConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "quartz" namespace. */
public class QuartzNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new QuartzConnectorDefinitionParser());
        registerBeanDefinitionParser("factory-properties", new ChildMapDefinitionParser("factoryProperties"));
    }
}

class QuartzConnectorDefinitionParser extends MuleOrphanDefinitionParser
{
    public QuartzConnectorDefinitionParser()
    {
        super(QuartzConnector.class, true);
        addAlias("scheduler", "quartzScheduler");
    }
}
