/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
