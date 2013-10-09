/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
