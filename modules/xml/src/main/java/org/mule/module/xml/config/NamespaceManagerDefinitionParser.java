/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.module.xml.util.NamespaceManager;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * TODO
 */
public class NamespaceManagerDefinitionParser extends OrphanDefinitionParser
{
    public NamespaceManagerDefinitionParser()
    {
        super(NamespaceManager.class, true);
    }

    /**
     * Parse the supplied {@link org.w3c.dom.Element} and populate the supplied
     * {@link org.springframework.beans.factory.support.BeanDefinitionBuilder} as required.
     * <p>The default implementation delegates to the <code>doParse</code>
     * version without ParserContext argument.
     *
     * @param element       the XML element being parsed
     * @param parserContext the object encapsulating the current state of the parsing process
     * @param builder       used to define the <code>BeanDefinition</code>
     */
    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Map<String, String> ns = new HashMap<String, String>();

        for (int i = 0; i < element.getParentNode().getAttributes().getLength(); i++)
        {
            Node node = element.getParentNode().getAttributes().item(i);
            String prefix = node.getNodeName();
            if (prefix.startsWith("xmlns"))
            {
                if (prefix.indexOf(":") > 0)
                {
                    prefix = prefix.substring(prefix.indexOf(":") + 1);
                }
                else
                {
                    prefix = "";
                }
                ns.put(prefix, node.getNodeValue());
            }
        }
        builder.addPropertyValue("configNamespaces", ns);

        // this id must match the bean name
        element.setAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID, MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER);

        super.doParse(element, parserContext, builder);
    }

    @Override
    public String getBeanName(Element element)
    {
        return MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER;
    }
}
