/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.AbstractHierarchicalDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * Processes child property elements in Xml but sets the properties on the parent object.  This is
 * useful when an object has lots of properties and it's more readable to break those properties into
 * groups that can be represented as a sub-element in Xml.
 */
public class ParentDefinitionParser extends AbstractHierarchicalDefinitionParser
{

    public static final String COMPOUND_ELEMENT = "compound";
    
    protected Class getBeanClass(Element element)
    {
        try
        {
            return Class.forName(getParentBeanDefinition(element).getBeanClassName());
        }
        catch (Exception e)
        {
            // Should continue to work, but automatic collection detection etc will fail
            logger.debug("No class for " + element);
            return Object.class;
        }
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        preProcess();
        setRegistry(parserContext.getRegistry());
        this.parserContext = parserContext;
        Class beanClass = getBeanClass(element);
        Assert.state(beanClass != null, "Class returned from getBeanClass(Element) must not be null, element is: " + element.getNodeName());
        BeanDefinitionBuilder builder = createBeanDefinitionBuilder(element, beanClass);
        builder.setSource(parserContext.extractSource(element));
        if (parserContext.isNested())
        {
            // Inner bean definition must receive same singleton status as containing bean.
            builder.setSingleton(parserContext.getContainingBeanDefinition().isSingleton());
        }
        doParse(element, parserContext, builder);
        BeanAssembler beanAssembler = getBeanAssembler(element, builder);
        beanAssembler.copyBeanToTarget();
        beanAssembler.getTarget().setAttribute(COMPOUND_ELEMENT, Boolean.TRUE);
        return (AbstractBeanDefinition) beanAssembler.getTarget();
    }

    protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element)
    {
        // by default the name matches the "real" bean
        if (null == element.getAttributeNode(ATTRIBUTE_NAME))
        {
            element.setAttribute(ATTRIBUTE_NAME, getParentBeanName(element));
        }
    }
}
