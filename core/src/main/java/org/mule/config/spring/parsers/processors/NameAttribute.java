/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.w3c.dom.Element;

public class NameAttribute implements PostProcessor
{

    private String name;

    public NameAttribute(String name)
    {
        this.name = name;
    }

    public void postProcess(BeanAssembler assembler, Element element)
    {
        element.setAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME, name);
    }

}