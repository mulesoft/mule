/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This Namespace handler extends the default Spring {@link org.springframework.beans.factory.xml.NamespaceHandlerSupport}
 * to allow certain elements in document to be ignorred by the handler.
 */
public abstract class AbstractIgnorableNamespaceHandler extends NamespaceHandlerSupport
{
    protected final void registerIgnoredElement(String name)
    {
        registerBeanDefinitionParser(name, new IgnorredDefinitionParser());
    }

    private class IgnorredDefinitionParser implements BeanDefinitionParser
    {

        public BeanDefinition parse(Element element, ParserContext parserContext)
        {
            return parserContext.getContainingBeanDefinition();
        }
    }
}