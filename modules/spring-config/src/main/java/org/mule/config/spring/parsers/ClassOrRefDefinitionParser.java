/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


