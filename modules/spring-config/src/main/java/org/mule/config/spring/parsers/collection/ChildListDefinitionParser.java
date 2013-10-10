/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ChildListDefinitionParser extends ChildDefinitionParser
{

    public ChildListDefinitionParser(String setterMethod)
    {
        super(setterMethod, ArrayList.class);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    }

    protected Class getBeanClass(Element element)
    {
        return ListFactoryBean.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        List parsedList = parserContext.getDelegate().parseListElement(element, builder.getRawBeanDefinition());
        builder.addPropertyValue("sourceList", parsedList);
        builder.addPropertyValue("targetListClass", super.getBeanClass(element));
    }

}
