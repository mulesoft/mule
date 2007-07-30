/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.impl.MuleDescriptor;
import org.mule.util.StringUtils;
import org.mule.util.object.SimpleObjectFactory;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser used for processing <code><mule:service></code> elements.
 */
public class ServiceDescriptorDefinitionParser extends AbstractMuleBeanDefinitionParser
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
        builder.setSingleton(true);
        builder.addDependsOn(modelName);

        // For backwards-compatibility only.
        String implClass = element.getAttribute("implementation");
        if (StringUtils.isNotBlank(implClass))
        {
            BeanDefinitionBuilder serviceFactory = BeanDefinitionBuilder.rootBeanDefinition(SimpleObjectFactory.class);
            serviceFactory.addPropertyValue("objectClassName", implClass);
            String serviceName = element.getAttribute("name") + "-factory";
            // Reference this bean from the service descriptor.
            builder.addPropertyReference("serviceFactory", serviceName);
            // Register the new bean.
            BeanDefinitionHolder holder = new BeanDefinitionHolder(serviceFactory.getBeanDefinition(), serviceName);
            registerBeanDefinition(holder, parserContext.getRegistry());
            element.removeAttribute("implementation");
        }
        
        super.doParse(element, parserContext, builder);
    }

}
