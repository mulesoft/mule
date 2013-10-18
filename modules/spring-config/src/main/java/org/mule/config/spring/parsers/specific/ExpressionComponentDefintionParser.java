/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ExpressionComponentDefintionParser extends ChildDefinitionParser
{
    public ExpressionComponentDefintionParser(String setterMethod, Class<?> clazz)
    {
        super(setterMethod, clazz);
        addAlias("file", "expressionFile");
    }

    @Override
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);
        if (element.getTextContent() != null && !element.getTextContent().isEmpty())
        {
            assembler.getBean().addPropertyValue("expression", element.getTextContent());
        }
    }
}
