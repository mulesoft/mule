/*
 * $Id:ConfigurationDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parses the <mule:configuration> element. If this element appears in multiple Xml config files each will its configuration
 * to a singe {@link MuleConfiguration} object.
 *
 * @see MuleConfiguration
 */
public class ConfigurationDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{

    public static final String ATTRIBUTE_SERVER_ID = "serverId";
    private String parent;

    public ConfigurationDefinitionParser()
    {
        registerAttributeMapping(ATTRIBUTE_SERVER_ID, ATTRIBUTE_ID);
        singleton=true;
    }

    protected Class getBeanClass(Element element)
    {
        return MuleConfiguration.class;
    }

    

    //@java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if(parent==null)
        {
            builder.addPropertyValue(ATTRIBUTE_ID, MuleProperties.OBJECT_MULE_CONFIGURATION);
            parent = MuleProperties.OBJECT_MULE_CONFIGURATION;
        }
        super.doParse(element, parserContext, builder);
    }

    //@Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return MuleProperties.OBJECT_MULE_CONFIGURATION;
    }


    //@java.lang.Override
    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        if(parent!=null)
        {
            return BeanDefinitionBuilder.childBeanDefinition(parent);
        }
        else
        {
            return super.createBeanDefinitionBuilder(element, beanClass);
        }
    }
}