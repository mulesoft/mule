/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
