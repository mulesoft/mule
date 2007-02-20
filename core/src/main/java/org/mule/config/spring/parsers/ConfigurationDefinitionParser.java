/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.MuleConfiguration;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
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


    @Override
    protected void processProperty(Attr attribute, BeanDefinitionBuilder builder)
    {
        if(attribute.getNodeName().equals(ATTRIBUTE_SERVER_ID))
        {
            attribute.setValue(attribute.getValue() + CONFIG_POSTFIX);
        }
            super.processProperty(attribute, builder);
    }


    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return element.getAttribute(ATTRIBUTE_SERVER_ID);
    }
}