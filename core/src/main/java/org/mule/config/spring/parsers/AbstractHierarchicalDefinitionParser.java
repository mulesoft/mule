/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
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

    protected String getParentBeanName(Element element)
    {
        return ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_NAME);
    }

    protected BeanDefinition getParentBeanDefinition(Element element)
    {
        String parentBean = getParentBeanName(element);
        if (StringUtils.isBlank(parentBean))
        {
            throw new IllegalStateException("No parent for " +
                    MuleHierarchicalBeanDefinitionParserDelegate.elementToString(element));
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
    public BeanAssembler getBeanAssembler(Element element, BeanDefinitionBuilder bean)
    {
        BeanDefinition target = getParentBeanDefinition(element);
        return beanAssemblerFactory.newBeanAssembler(
                propertyConfiguration, bean, propertyConfiguration, target);
    }

}
