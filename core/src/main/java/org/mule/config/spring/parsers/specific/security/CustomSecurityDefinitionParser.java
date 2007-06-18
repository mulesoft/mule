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

import org.mule.config.spring.parsers.general.CompoundElementDefinitionParser;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class CustomSecurityDefinitionParser extends CompoundElementDefinitionParser
{

    public static final String NAME = "name";

    private String name;
    private String componentAttributeValue;
    private String componentAttributeName;
    private ParserContext parserContext;

    public CustomSecurityDefinitionParser(String componentAttributeName)
    {
        this.componentAttributeName = componentAttributeName;
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

    // we don't know which will be set first and want to avoid setting name on the
    // manager itself...
    protected void addProperty(BeanDefinitionBuilder builder, String name, String value, boolean reference)
    {
        if (NAME.equals(name))
        {
            this.name = value;
            if (null != componentAttributeValue)
            {
                setName();
            }
        }
        else {
            super.addProperty(builder, name, value, reference);
            if (componentAttributeName.equals(name))
            {
                componentAttributeValue = value;
                if (null != this.name)
                {
                    setName();
                }
            }
        }
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

}