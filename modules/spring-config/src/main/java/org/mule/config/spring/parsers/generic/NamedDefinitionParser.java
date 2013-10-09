/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Behaves as {@link org.mule.config.spring.parsers.generic.ParentDefinitionParser},
 * but allows any named bean to be the parent, rather than using the enclosing element in the DOM tree.
 */
public class NamedDefinitionParser extends ParentDefinitionParser
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

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);
        // may be used as top level element, so set ID from name
        AutoIdUtils.ensureUniqueId(element, "named");
    }
}
