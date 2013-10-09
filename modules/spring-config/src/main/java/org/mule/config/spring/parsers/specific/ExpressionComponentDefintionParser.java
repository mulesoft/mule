/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
