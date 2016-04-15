/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.util.SpringXMLUtils;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A parser for direct children of the <mule> element.
 */
public class MuleOrphanDefinitionParser extends OrphanDefinitionParser
{
    /**
     * This constructor assumes that the class name will be explicitly specified as
     * an attribute on the element.
     */
    public MuleOrphanDefinitionParser(boolean singleton)
    {
        super(singleton);
    }

    public MuleOrphanDefinitionParser(Class<?> beanClass, boolean singleton)
    {
        super(beanClass, singleton);
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        assertMuleParent(element);
        return super.parseInternal(element, parserContext);
    }

    protected void assertMuleParent(Element element)
    {
        if (!isTopLevel(element))
        {
            throw new IllegalStateException("This element should be embedded inside the Mule <"
                    + ROOT_ELEMENT + "> element: " + SpringXMLUtils.elementToString(element));
        }
    }
}
