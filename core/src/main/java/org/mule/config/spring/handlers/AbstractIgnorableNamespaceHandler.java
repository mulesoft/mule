/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractDelegatingDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This Namespace handler extends the default Spring {@link org.springframework.beans.factory.xml.NamespaceHandlerSupport}
 * to allow certain elements in document to be ignored by the handler.
 */
public abstract class AbstractIgnorableNamespaceHandler extends NamespaceHandlerSupport
{

    /**
     * @param name The name of the element to be ignored.
     */
    protected final void registerIgnoredElement(String name)
    {
        registerBeanDefinitionParser(name, new IgnoredDefinitionParser());
    }

    protected final MuleDefinitionParser registerMuleDefinitionParser(String name, AbstractMuleBeanDefinitionParser parser)
    {
        super.registerBeanDefinitionParser(name, parser);
        return parser;
    }

    protected final MuleDefinitionParser registerDelegateDefinitionParser(String name, AbstractDelegatingDefinitionParser parser)
    {
        super.registerBeanDefinitionParser(name, parser);
        return parser;
    }

    private class IgnoredDefinitionParser implements BeanDefinitionParser
    {
        public BeanDefinition parse(Element element, ParserContext parserContext)
        {
            return null;
        }
    }

}