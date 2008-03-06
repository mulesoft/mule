/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * This iterates over child elements, parsing them and calling
 * {@link #insertBean(org.mule.config.spring.parsers.assembly.BeanAssembler, org.springframework.beans.factory.config.BeanDefinition, org.w3c.dom.Element, org.w3c.dom.Element)}.
 *
 * <p>There are two ways we can parse a tree of elements - have an external loop or let each parser iterate
 * over its own children.  Mule uses the first strategy, but some (most? all?) third party BDPs use the
 * second.  This processor lets us use third party beans inside the Mule framework.
 *
 * <p>So this is a very specialised parser that should only be used when trying to inter-operate with beans from
 * third party packages which themselves control how their children are parsed.
 *
 * <p>Since for Mule beans the iteration over child elements (at least currently) is done via
 * {@link org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate} the calling parser needs to set
 * the flag {@link org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate#MULE_NO_RECURSE} - this
 * stops the Mule recursion from working.
 *
 * <p>NOTE - IMHO (ac) the Mule approach was probably a mistake; this processor could be used as a way to
 * slowly migrate the Mule code to the more common approach.
 */
public abstract class AbstractChildElementIterator implements PostProcessor
{

    private BeanAssemblerFactory beanAssemblerFactory;
    private PropertyConfiguration configuration;

    public AbstractChildElementIterator(BeanAssemblerFactory beanAssemblerFactory, PropertyConfiguration configuration)
    {
        this.beanAssemblerFactory = beanAssemblerFactory;
        this.configuration = configuration;
    }

    public void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                processChildElement(context, assembler, element, (Element) child);
            }
        }
    }

    protected void processChildElement(ParserContext context, BeanAssembler assembler, Element parent, Element child)
    {
        BeanDefinition childBean = null;
        
        if ("http://www.springframework.org/schema/beans".equals(child.getNamespaceURI())) 
        {
            childBean = context.getDelegate().parseBeanDefinitionElement(child).getBeanDefinition();
        } 
        else 
        {
            childBean = context.getDelegate().parseCustomElement(child, assembler.getBean().getBeanDefinition());
        }
        BeanAssembler targetAssembler = beanAssemblerFactory.newBeanAssembler(null, null,
                configuration, assembler.getBean().getRawBeanDefinition());
        insertBean(targetAssembler, childBean, parent, child);
    }

    protected abstract void insertBean(BeanAssembler targetAssembler, BeanDefinition childBean, Element parent, Element child);

}
