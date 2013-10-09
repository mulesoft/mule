/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
