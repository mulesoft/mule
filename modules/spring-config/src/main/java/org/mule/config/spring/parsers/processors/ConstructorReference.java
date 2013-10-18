/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.ParserContext;

public class ConstructorReference implements PostProcessor
{

    public String reference;

    public ConstructorReference(String reference)
    {
        this.reference = reference;
    }

    public void postProcess(ParserContext unused, BeanAssembler assembler, Element element)
    {
        assembler.getBean().addConstructorArgReference(reference);
    }

}
