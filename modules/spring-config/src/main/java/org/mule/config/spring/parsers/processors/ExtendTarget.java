/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.ParserContext;

public class ExtendTarget implements PostProcessor
{

    private String name;
    private String value;

    public ExtendTarget(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public void postProcess(ParserContext unused, BeanAssembler assembler, Element element)
    {
        Attr attribute = element.getOwnerDocument().createAttribute(name);
        attribute.setNodeValue(value);
        assembler.extendTarget(attribute);
    }

}
