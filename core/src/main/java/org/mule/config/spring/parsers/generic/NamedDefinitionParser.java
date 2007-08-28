/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.generic;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Behaves as {@link org.mule.config.spring.parsers.generic.ParentDefinitionParser},
 * but allows any named bean to be the parent, rather than using the enclosing element in the DOM tree.
 */
public class NamedDefinitionParser extends ParentDefinitionParser implements DelegateDefinitionParser
{

    private String name;
    private boolean isDynamic = false;

    public NamedDefinitionParser()
    {
        isDynamic = true;
    }

    public NamedDefinitionParser(String name)
    {
        addIgnored(ATTRIBUTE_NAME);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected String getParentBeanName(Element element)
    {
        return name;
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        if (isDynamic)
        {
            if (element.hasAttribute(ATTRIBUTE_NAME))
            {
                setName(element.getAttribute(ATTRIBUTE_NAME));
                element.removeAttribute(ATTRIBUTE_NAME);
            }
            else
            {
                throw new IllegalStateException("Missing name attribute for " + element.getLocalName());
            }
        }
        return super.parseInternal(element, parserContext);
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

}
