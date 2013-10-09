/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.config;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.processors.ProvideDefaultNameFromElement;
import org.mule.module.management.agent.JmxAgentConfigurer;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JmxAgentDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    public static final String CONNECTOR_SERVER = "connector-server";

    public JmxAgentDefinitionParser()
    {
        singleton = true;
        addAlias("server", "mBeanServer");
        registerPreProcessor(new ProvideDefaultNameFromElement());
    }

    protected Class<?> getBeanClass(Element element)
    {
        return JmxAgentConfigurer.class;
    }

    @Override
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (CONNECTOR_SERVER.equals(node.getLocalName()))
            {
                assembler.extendBean("connectorServerUrl", ((Element) node).getAttribute("url"), false);
                String rebind = ((Element) node).getAttribute("rebind");

                if (!StringUtils.isEmpty(rebind))
                {
                    Map<String, String> csProps = new HashMap<String, String>();
                    csProps.put("jmx.remote.jndi.rebind", rebind);
                    assembler.extendBean("connectorServerProperties", csProps, false);
                }
            }
        }

        super.postProcess(context, assembler, element);
    }

}
