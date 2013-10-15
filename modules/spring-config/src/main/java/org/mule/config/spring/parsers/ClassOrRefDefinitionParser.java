/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ClassOrRefDefinitionParser extends AbstractBeanDefinitionParser
{
    private String propertyName;

    public ClassOrRefDefinitionParser(String propertyName)
    {
        super();
        
        if (StringUtils.isEmpty(propertyName))
        {
            throw new IllegalArgumentException("propertyName cannot be null");
        }
        this.propertyName = propertyName;
    }
    
    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        MutablePropertyValues parentProps = parserContext.getContainingBeanDefinition().getPropertyValues();

        String ref = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
        String className = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS);

        if (StringUtils.isBlank(ref) && StringUtils.isBlank(className))
        {
            String elementName = element.getLocalName();
            throw new IllegalArgumentException("Neither ref nor class attribute specified for the "
                + elementName + " element");
        }
        
        if (StringUtils.isNotBlank(ref))
        {
            // add a ref to other bean
            parentProps.addPropertyValue(propertyName, new RuntimeBeanReference(ref));
        }
        else
        {
            // class attributed specified, instantiate and set directly
            Object instance;
            try
            {
                instance = ClassUtils.instanciateClass(className, ClassUtils.NO_ARGS, getClass());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            parentProps.addPropertyValue(propertyName, instance);
        }

        return null;
    }
}


