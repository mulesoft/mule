/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Grabs the text from an element and injects it into the parent, for example:
 * 
 * <foo>
 *   <bar-text>A bunch of text.</bar-text>
 * </foo>
 * 
 *   registerBeanDefinitionParser("foo", new OrphanDefinitionParser(Foo.class));
 *   registerBeanDefinitionParser("bar-text", new TextDefinitionParser("barText"));
 * 
 * will result in a call to Foo.setBarText("A bunch of text.")
 */
public class TextDefinitionParser extends ChildDefinitionParser
{
    private boolean requireCdata = false;

    public TextDefinitionParser(String setterMethod)
    {
        super(setterMethod, String.class);
    }

     public TextDefinitionParser(String setterMethod, boolean requireCdata)
    {
        super(setterMethod, String.class);
        this.requireCdata = requireCdata;
    }

    @Override
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {        
        Node node = element.getFirstChild();

        if(requireCdata && node.getNodeType() != Node.CDATA_SECTION_NODE)
        {
            node = node.getNextSibling();
            if(node == null)
            {
                 throw new IllegalArgumentException("No CDATA node found in " + element.getNodeName());
            }
            else if(node.getNodeType() != Node.CDATA_SECTION_NODE)
            {
                 throw new IllegalArgumentException("Sibling node is not a CDATA section, but one should be defined. Elements is " + element.getNodeName());
            }
        }
        if (node != null)
        {
            String value = node.getNodeValue();
            if (!StringUtils.isBlank(value))
            {
                assembler.getTarget().getPropertyValues().addPropertyValue(setterMethod, value);
            }
        }
    }
}
