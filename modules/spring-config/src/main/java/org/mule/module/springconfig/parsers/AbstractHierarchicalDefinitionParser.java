/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import org.mule.module.springconfig.parsers.assembly.BeanAssembler;
import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.module.springconfig.parsers.assembly.configuration.ReusablePropertyConfiguration;
import org.mule.module.springconfig.parsers.assembly.configuration.TempWrapperPropertyConfiguration;
import org.mule.module.springconfig.util.SpringXMLUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This definition parser introduces the notion of Hierarchical processing to nested XML elements. Definition
 * parsers that extend this can refer to parent beans.  It does not assume that the parser is restricted
 * to a single property.
 *
 * Calling classes must set the registry at the start of processing.
 *
 * @see org.mule.module.springconfig.parsers.generic.ChildDefinitionParser
 * @see org.mule.module.springconfig.parsers.collection.ChildMapEntryDefinitionParser.KeyValuePair
 * @see org.mule.module.springconfig.parsers.AbstractMuleBeanDefinitionParser
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

    public BeanDefinition getParentBeanDefinition(Element element)
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
                throw new IllegalStateException("No parent for " + SpringXMLUtils.elementToString(element));
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
        return getBeanAssemblerFactory().newBeanAssembler(
                beanPropertyConfiguration, bean, targetPropertyConfiguration, target);
    }

    /**
     * Provide access to bean assembler from non-hierarchical case.  Legacy support for
     * "mixed" definition parsers.
     *
     * @deprecated
     * @param element
     * @param bean
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
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);
        forcedParent = null;
    }
    
}
