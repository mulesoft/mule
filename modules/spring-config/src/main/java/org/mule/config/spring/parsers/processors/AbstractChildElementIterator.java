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
        BeanDefinition childBean = context.getDelegate().parseCustomElement(child, assembler.getBean().getBeanDefinition());
        BeanAssembler targetAssembler = beanAssemblerFactory.newBeanAssembler(null, null,
                configuration, assembler.getBean().getRawBeanDefinition());
        insertBean(targetAssembler, childBean, parent, child);
    }

    protected abstract void insertBean(BeanAssembler targetAssembler, BeanDefinition childBean, Element parent, Element child);

}
