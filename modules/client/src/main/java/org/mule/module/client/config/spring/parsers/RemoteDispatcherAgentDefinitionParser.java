/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.config.spring.parsers;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.AgentDefinitionParser;
import org.mule.module.client.remoting.RemoteDispatcherAgent;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class RemoteDispatcherAgentDefinitionParser extends AgentDefinitionParser
{

    public RemoteDispatcherAgentDefinitionParser()
    {
        super(RemoteDispatcherAgent.class);

    }

}
