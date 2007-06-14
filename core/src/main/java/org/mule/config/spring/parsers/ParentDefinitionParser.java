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

import org.mule.util.ClassUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * This extends information on an already-defined bean.  I think it should be possible to do this
 * within the existing classes, but I tried a couple of times and failed - they are too much
 * centred around a bean definition builder for a new class.
 */
public class ParentDefinitionParser extends AbstractBeanDefinitionParser
{

    private String parentName;
    private Class parentClass;
    protected PropertyToolkit propertyToolkit = new PropertyToolkit();

    public ParentDefinitionParser(String parentName, Class parentClass)
    {
        this.parentName = parentName;
        this.parentClass = parentClass;
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        AbstractBeanDefinition beanDef = getParentBean(parserContext);
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i)
        {
            processProperty((Attr) attributes.item(i), beanDef, parserContext);
        }
        return beanDef;
    }

    protected void processProperty(Attr attribute, BeanDefinition beanDef, ParserContext parserContext)
    {
        boolean isBeanReference = propertyToolkit.isBeanReference(attribute.getNodeName());
        String propertyName = propertyToolkit.extractPropertyName(attribute.getNodeName());
        String propertyValue = propertyToolkit.extractPropertyValue(propertyName, attribute.getValue());
        Assert.state(StringUtils.hasText(propertyName),
                "Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");

        if (isBeanReference)
        {
            AbstractBeanDefinition ref =
                    (AbstractBeanDefinition)parserContext.getRegistry().getBeanDefinition(propertyValue);
            setProperty(beanDef, propertyName, ref);
        }
        else
        {
            setProperty(beanDef, propertyName, propertyValue);
        }
    }

    protected void setProperty(BeanDefinition target, String name, Object value)
    {
        MutablePropertyValues propertyValues = target.getPropertyValues();
        if (propertyToolkit.isCollection(name))
        {
            Collection values = new HashSet();
            if (propertyValues.contains(name))
            {
                values = (Collection) propertyValues.getPropertyValue(name).getValue();
                propertyValues.removePropertyValue(name);
            }
            values.add(value);
            target.getPropertyValues().addPropertyValue(name, values);
        }
        else
        {
            target.getPropertyValues().addPropertyValue(name, value);
        }
    }

    public void registerBeanReference(String propertyName)
    {
        propertyToolkit.registerBeanReference(propertyName);
    }

    public void registerValueMapping(PropertyToolkit.ValueMap mapping)
    {
        propertyToolkit.registerValueMapping(mapping);
    }

    public void registerValueMapping(String propertyName, Map mappings)
    {
        propertyToolkit.registerValueMapping(propertyName, mappings);
    }

    public void registerValueMapping(String propertyName, String mappings)
    {
        propertyToolkit.registerValueMapping(propertyName, mappings);
    }

    public void registerAttributeMapping(String alias, String propertyName)
    {
        propertyToolkit.registerAttributeMapping(alias, propertyName);
    }

    public void registerList(String propertyName)
    {
        propertyToolkit.registerCollection(propertyName);
    }

    protected AbstractBeanDefinition getParentBean(ParserContext parserContext)
    {
        AbstractBeanDefinition beanDef =
                (AbstractBeanDefinition)parserContext.getRegistry().getBeanDefinition(parentName);
        try
        {
            Class beanClass = ClassUtils.getClass(beanDef.getBeanClassName());
            if (!(parentClass.isAssignableFrom(beanClass))) {
                throw new IllegalArgumentException("Class for " + parentName + " not a subclass of " + parentClass);
            }
            return beanDef;
        }
        catch (ClassNotFoundException e)
        {
            throw (IllegalStateException) (new IllegalStateException("Bean does not exist!")).initCause(e);
        }
    }

}