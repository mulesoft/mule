/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.scripting.config;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

import org.w3c.dom.Element;

public class ScriptDefinitionParser extends MuleOrphanDefinitionParser
{

    public ScriptDefinitionParser(boolean singleton)
    {
        super(singleton);
        addIgnored("name");
    }

    protected Class getBeanClass(org.w3c.dom.Element element) 
    {
        return String.class;
    }
    
    protected void postProcess(BeanAssembler assembler, Element element)
    {
        assembler.getBean().addConstructorArg(element.getFirstChild().getNodeValue());
        super.postProcess(assembler, element);
    }

}


