/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ognl.config;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.module.ognl.filters.OGNLFilter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Registers Bean Definition Parsers for the "ognl" namespace.
 */
public class OGNLNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("filter", new FilterDefinitionParser(OGNLFilter.class));
        registerBeanDefinitionParser("expression", new CDATABeanDefinitionParser("expression", String.class));
    }

    private static class CDATABeanDefinitionParser extends
                                                   ChildDefinitionParser
    {
        private CDATABeanDefinitionParser(String setterMethod, Class clazz)
        {
            super(setterMethod, clazz);
        }

        protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
        {
            assembler.extendTarget(setterMethod, element.getFirstChild().getNodeValue(), false);
        }
    }
}
