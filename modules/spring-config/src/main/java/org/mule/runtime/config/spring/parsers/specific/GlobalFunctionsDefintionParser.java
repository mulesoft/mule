/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
