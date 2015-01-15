/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.assembly.BeanAssembler;
import org.mule.module.springconfig.parsers.generic.MuleOrphanDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.SystemPropertyUtils;
import org.w3c.dom.Element;

public class GlobalPropertyDefinitionParser extends MuleOrphanDefinitionParser
{
    private final String VALUE_ATTR = "value";
    private final String NAME_ATTR = "name";

    public GlobalPropertyDefinitionParser()
    {
        super(true);
        addIgnored(NAME_ATTR);
        addIgnored(VALUE_ATTR);
    }

    protected Class getBeanClass(Element element)
    {
        return String.class;
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        String name = element.getAttribute(NAME_ATTR);
        if(name.indexOf(' ') != -1)
        {
            logger.warn("Environment property name should not contain spaces: \"" + name + "\"");
        }

        String value = element.getAttribute(VALUE_ATTR);
        assembler.getBean().addConstructorArgValue(SystemPropertyUtils.resolvePlaceholders(value));
        super.postProcess(context, assembler, element);
    }
}
