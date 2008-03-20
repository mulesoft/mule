/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ComponentDefinitionParser extends ChildDefinitionParser
{

    public ComponentDefinitionParser(Class clazz)
    {
        super("component", clazz);
        this.singleton = true;
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS);
    }

    // @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Element parent = (Element) element.getParentNode();
        String serviceName = parent.getAttribute(ATTRIBUTE_NAME);
        builder.addPropertyReference("service", serviceName);
        super.parseChild(element, parserContext, builder);
    }
}
