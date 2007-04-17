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

import org.mule.impl.MuleDescriptor;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser used for processing <code><mule:service></code> elements.
 */
public class ServiceDescriptorDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    protected Class getBeanClass(Element element)
    {
        return MuleDescriptor.class;
    }

    //@java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Element parent = (Element) element.getParentNode();
        String modelName = parent.getAttribute(ATTRIBUTE_NAME);
        builder.addPropertyValue("modelName", modelName);
        builder.getBeanDefinition().setEnforceDestroyMethod(false);
        builder.getBeanDefinition().setEnforceInitMethod(false);
        builder.setInitMethodName("initialise");
        builder.setDestroyMethodName("dispose");
        builder.setSingleton(true);
        builder.addDependsOn(modelName);

        super.doParse(element, parserContext, builder);
    }
}
