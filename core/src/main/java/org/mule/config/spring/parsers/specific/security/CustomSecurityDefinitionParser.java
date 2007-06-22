/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.security;

import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class  CustomSecurityDefinitionParser extends ParentDefinitionParser
{

    public static final String NAME = "name";

    private String name;
    private String componentAttributeValue;
    private String componentAttributeName;
    private ParserContext parserContext;

    public CustomSecurityDefinitionParser(String componentAttributeName)
    {
        this.componentAttributeName = componentAttributeName;
        this.beanAssemblerFactory = new LocalBeanAssemblerFactory();
    }

    // this is a bit of a hack - we transfer the name to the provider

    // reset for each use
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        name = null;
        componentAttributeValue = null;
        this.parserContext = parserContext;
        return super.parseInternal(element, parserContext);
    }


    //  only set name if not already given
    private void setName()
    {
        BeanDefinition beanDef = parserContext.getRegistry().getBeanDefinition(componentAttributeValue);
        MutablePropertyValues propertyValues = beanDef.getPropertyValues();
        if (!propertyValues.contains(NAME))
        {
            propertyValues.addPropertyValue(NAME, name);
        }
    }

    private class LocalBeanAssembler extends DefaultBeanAssembler
    {

        public LocalBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                      PropertyConfiguration targetConfig, BeanDefinition target)
        {
            super(beanConfig, bean, targetConfig, target);
        }

        public void extendBean(String newName, Object newValue, boolean isReference)
        {
            if (NAME.equals(newName) && newValue instanceof String)
            {
                name = (String) newValue;
                if (null != componentAttributeValue)
                {
                    setName();
                }
            }
            else {
                super.extendBean(newName, newValue, isReference);
                if (componentAttributeName.equals(newName) && newValue instanceof String)
                {
                    componentAttributeValue = (String) newValue;
                    if (null != name)
                    {
                        setName();
                    }
                }
            }
        }

    }

    private class LocalBeanAssemblerFactory implements BeanAssemblerFactory
    {

        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                                      PropertyConfiguration targetConfig, BeanDefinition target)
        {
            return new LocalBeanAssembler(beanConfig, bean, targetConfig, target);
        }

    }

}