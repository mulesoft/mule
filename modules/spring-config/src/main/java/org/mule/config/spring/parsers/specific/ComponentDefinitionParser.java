/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.w3c.dom.Element;

public class ComponentDefinitionParser extends ChildDefinitionParser
{

    public ComponentDefinitionParser(Class clazz)
    {
        super("messageProcessor", clazz);
        this.singleton = false;
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS);
    }

    @Override
    public String getPropertyName(Element e)
    {
        String parent = e.getParentNode().getLocalName().toLowerCase();
        if ("service".equals(parent) || "custom-service".equals(parent))
        {
            return "component";
        }
        else
        {
            return super.getPropertyName(e);
        }
    }
}
