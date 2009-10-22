/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parser.specific;

import org.mule.config.spring.factories.AnnotatedServiceFactoryBean;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Creates an {@link org.mule.api.service.Service} object from an annotated class. Note that it is the
 * {@link org.mule.config.spring.factories.AnnotatedServiceFactoryBean} that orchestrated the creation
 * of the Service.
 */
public class AnnotatedServiceDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    protected Class getBeanClass(Element element)
    {
        return AnnotatedServiceFactoryBean.class;
    }

    //@java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {   
        Element parent = (Element) element.getParentNode();
        String modelName = parent.getAttribute(ATTRIBUTE_NAME);
        builder.addPropertyReference("model", modelName);
        builder.setSingleton(true);
        builder.setLazyInit(false);
        //builder.addDependsOn(modelName);

        super.doParse(element, parserContext, builder);
    }

}

