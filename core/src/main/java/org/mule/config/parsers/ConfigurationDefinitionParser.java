/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.config.MuleConfiguration;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class ConfigurationDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{

    public static final String ATTRIBUTE_SERVER_ID = "serverId";
    public static final String CONFIG_POSTFIX = ".config";


    public ConfigurationDefinitionParser()
    {
        registerAttributeMapping(ATTRIBUTE_SERVER_ID, ATTRIBUTE_ID);
    }

    protected Class getBeanClass(Element element)
    {
        return MuleConfiguration.class;
    }


    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String serverId = element.getAttribute(ATTRIBUTE_SERVER_ID);
        element.setAttribute(ATTRIBUTE_ID, serverId + CONFIG_POSTFIX);
        super.doParse(element, parserContext, builder);
    }
}
