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
import org.mule.config.spring.parsers.assembly.ReusablePropertyConfiguration;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.TempWrapperPropertyConfiguration;
import org.mule.util.StringUtils;
import org.mule.util.CoreXMLUtils;

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

    private ReusablePropertyConfiguration targetPropertyConfiguration =
            new ReusablePropertyConfiguration(
                    new TempWrapperPropertyConfiguration(beanPropertyConfiguration, false));
    private BeanDefinition forcedParent = null;

    public PropertyConfiguration getTargetPropertyConfiguration()
    {
        return targetPropertyConfiguration;
    }

    protected String getParentBeanName(Element element)
    {
        return ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_NAME);
    }

    protected BeanDefinition getParentBeanDefinition(Element element)
    {
        if (null != forcedParent)
        {
            return forcedParent;
        }
        else
        {
            String parentBean = getParentBeanName(element);
            if (StringUtils.isBlank(parentBean))
            {
                throw new IllegalStateException("No parent for " +
                        CoreXMLUtils.elementToString(element));
            }
            return getRegistry().getBeanDefinition(parentBean);
        }
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
                beanPropertyConfiguration, bean, targetPropertyConfiguration, target);
    }

    /**
     * Provide access to bean assembler from non-hierarchical case.  Legacy support for
     * "mixed" definition parsers.
     *
     * @deprecated
     * @param element
     * @param bean
     * @return
     */
    protected BeanAssembler getOrphanBeanAssembler(Element element, BeanDefinitionBuilder bean)
    {
        return super.getBeanAssembler(element, bean);
    }

    public void forceParent(BeanDefinition parent)
    {
        forcedParent = parent;
    }

    protected void preProcess(Element element)
    {
        super.preProcess(element);
        targetPropertyConfiguration.reset();
    }

    // reset the forced parent
    protected void postProcess(BeanAssembler assembler, Element element)
    {
        super.postProcess(assembler, element);
        forcedParent = null;
    }
    
}
