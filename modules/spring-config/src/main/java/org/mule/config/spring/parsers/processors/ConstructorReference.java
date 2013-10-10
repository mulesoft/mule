/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
