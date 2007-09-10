/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.management.agents.JmxAgent;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TODO
 */
public class JmxAgentDefinitionParser extends AbstractMuleBeanDefinitionParser
{

    public static final String CONNECTOR_SERVER = "connector-server";


    public JmxAgentDefinitionParser()
    {
        singleton = true;
    }

    protected Class getBeanClass(Element element) {
        return JmxAgent.class;
    }


    protected void postProcess(BeanAssembler assembler, Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (CONNECTOR_SERVER.equals(node.getLocalName())) {
                assembler.extendBean("connectorServerUrl", ((Element) node).getAttribute("url"), false);
                String rebind = ((Element) node).getAttribute("rebind");
                if (!StringUtils.isEmpty(rebind)) {
                    Map csProps = new HashMap();
                    csProps.put("jmx.remote.jndi.rebind", rebind);
                    assembler.extendBean("connectorServerProperties", csProps, false);
                }
            }
        }
        super.postProcess(assembler, element);
    }
}
