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

import org.mule.util.StringUtils;
import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * This definition parser introduces the notion of Hierarchical processing to nested XML elements. Definition
 * parsers that extend this can refer to parent beans.  It does not assume that the parser is restricted
 * to a single property.
 *
 * Calling classes must set the registry at the start of processing.
 *
 * @see org.mule.config.spring.parsers.generic.ChildDefinitionParser
 * @see org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser.KeyValuePair
 * @see org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser
 */
public abstract class AbstractHierarchicalDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private BeanDefinitionRegistry registry;

    /**
     * This must be set at the start of processing (ie from within doParse).
     *
     * @param registry
     */
    protected void setRegistry(BeanDefinitionRegistry registry)
    {
        this.registry = registry;
    }

    protected BeanDefinitionRegistry getRegistry()
    {
        if (null == registry)
        {
            throw new IllegalStateException("Set the registry from within doParse");
        }
        return registry;
    }

    protected void preProcess()
    {
        registry = null;
        super.preProcess();
    }

    protected String getParentBeanName(Element element)
    {
        return ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_NAME);
    }

    protected BeanDefinition getParentBeanDefinition(Element element)
    {
        String parentBean = getParentBeanName(element);
        if (StringUtils.isBlank(parentBean))
        {
            // MULE-1737 - this used to be just a printed message and a return from
            // the parser.  Now it's in a separate method it's simpler to have an
            // exception and fix the incorrect use
            throw new IllegalStateException("Bean: " + element.getNodeName() + " has no parent");
        }
        return getRegistry().getBeanDefinition(parentBean);
    }

    /**
     * The bean assembler gives more reliable/automatic processing of collections, maps, etc.
     *
     * @param element The current element
     * @param bean The bean being constructed
     * @return An assembler that includes Mule-specific construction logic
     */
    protected BeanAssembler getBeanAssembler(Element element, BeanDefinitionBuilder bean)
    {
        BeanDefinition target = getParentBeanDefinition(element);
        return beanAssemblerFactory.newBeanAssembler(
                propertyConfiguration, bean, propertyConfiguration, target);
    }

}
