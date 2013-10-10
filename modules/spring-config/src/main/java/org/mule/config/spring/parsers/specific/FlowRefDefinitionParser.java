/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.FlowRefFactoryBean;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class FlowRefDefinitionParser extends ChildDefinitionParser
{

    public FlowRefDefinitionParser()
    {
        super("messageProcessor", FlowRefFactoryBean.class);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
    }

    public String getBeanName(Element element)
    {
        return AutoIdUtils.uniqueValue("flow-ref." + element.getAttribute(ATTRIBUTE_NAME));
    }

    @Override
    protected void checkElementNameUnique(Element element)
    {
        // We want to check element name exists
    }

}
