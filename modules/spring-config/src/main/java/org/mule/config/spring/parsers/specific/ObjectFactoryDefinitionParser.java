/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.object.AbstractObjectFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

public class ObjectFactoryDefinitionParser extends AbstractChildDefinitionParser
{
    
    protected Class beanClass = null;
    protected String setterMethod = null;

    public ObjectFactoryDefinitionParser(Class beanClass, String setterMethod)
    {
        this(beanClass);
        this.setterMethod = setterMethod;
    }                                                             
    
    public ObjectFactoryDefinitionParser(Class beanClass)
    {
        super();
        this.beanClass = beanClass;
        setAllowClassAttribute(false);
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME);
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "factoryBean");
    }

    public String getPropertyName(Element element)
    {
        if (setterMethod != null)
        {
            return setterMethod;
        }
        else
        {
            BeanDefinition parent = getParentBeanDefinition(element);
            String setter = (String) parent.getAttribute(ObjectFactoryWrapper.OBJECT_FACTORY_SETTER);
            return setter;
        }
    }

    protected Class getBeanClass(Element element)
    {
        return beanClass;
    }                                                                 
}
