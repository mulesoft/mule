/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class ChildListDefinitionParser extends ChildDefinitionParser {

    public ChildListDefinitionParser(String setterMethod)
    {
        super(setterMethod, ArrayList.class);
    }

    public ChildListDefinitionParser(String setterMethod, Class listType)
    {
        super(setterMethod, listType, List.class);
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
        getBeanAssembler(element, builder).setBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    }

}
