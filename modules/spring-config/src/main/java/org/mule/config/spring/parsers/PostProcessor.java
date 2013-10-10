/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * This interface allows post-processing of the bean assmebler to be injected into
 * definition parsers
 */
public interface PostProcessor
{

    public void postProcess(ParserContext context, BeanAssembler assembler, Element element);

}
