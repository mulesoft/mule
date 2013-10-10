/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
