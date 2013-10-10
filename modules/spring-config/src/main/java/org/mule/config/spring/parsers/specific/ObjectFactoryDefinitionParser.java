/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
