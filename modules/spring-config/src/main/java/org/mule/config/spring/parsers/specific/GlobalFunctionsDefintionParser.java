/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class GlobalFunctionsDefintionParser extends TextDefinitionParser
{

    private static String FUNCTION_FILE_ATTRIBUTE_NAME = "file";

    public GlobalFunctionsDefintionParser(String setterMethod)
    {
        super(setterMethod);
    }

    @Override
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);
        if (element.hasAttribute(FUNCTION_FILE_ATTRIBUTE_NAME))
        {
            assembler.getTarget()
                .getPropertyValues()
                .add("globalFunctionsFile", element.getAttribute(FUNCTION_FILE_ATTRIBUTE_NAME));
        }
    }
}
