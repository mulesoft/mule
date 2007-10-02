/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.impl.internal.admin.MuleAdminAgent;

import org.w3c.dom.Element;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.BeanDefinitionStoreException;

public class MuleAdminAgentDefinitionParser extends MuleChildDefinitionParser
{

    public MuleAdminAgentDefinitionParser()
    {
        super(MuleAdminAgent.class, true);

    }

    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return MuleAdminAgent.AGENT_NAME;
    }
}
