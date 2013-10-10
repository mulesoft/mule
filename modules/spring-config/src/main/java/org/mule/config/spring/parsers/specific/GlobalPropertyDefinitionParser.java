/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

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
