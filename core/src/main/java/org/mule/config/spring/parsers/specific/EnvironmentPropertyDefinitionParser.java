/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;

import org.springframework.util.SystemPropertyUtils;
import org.w3c.dom.Element;

public class EnvironmentPropertyDefinitionParser extends MuleChildDefinitionParser
{
    private final String VALUE_ATTR = "value";
    private final String NAME_ATTR = "name";

    public EnvironmentPropertyDefinitionParser()
    {
        super(true);
        addIgnored(NAME_ATTR);
        addIgnored(VALUE_ATTR);
    }

    protected Class getBeanClass(Element element)
    {
        return String.class;
    }

    protected void postProcess(BeanAssembler assembler, Element element)
    {
        String name = element.getAttribute(NAME_ATTR);
        if(name.indexOf(' ') != -1)
        {
            logger.warn("Environment property name should not contain spaces: \"" + name + "\"");
        }

        String value = element.getAttribute(VALUE_ATTR);
        assembler.getBean().addConstructorArg(SystemPropertyUtils.resolvePlaceholders(value));
        super.postProcess(assembler, element);
    }
}
