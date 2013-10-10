/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.util.StringUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An extension to {@link ChildDefinitionParser} which recurses up the DOM
 * tree until it finds a named parent.
 */
public class DescendentDefinitionParser extends ChildDefinitionParser
{
    public DescendentDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }

    protected String getParentBeanName(Element element)
    {
        Node node = element;
        while (null != node && node instanceof Element)
        {
            String name = super.getParentBeanName((Element) node);
            if (!StringUtils.isBlank(name))
            {
                return name;
            }
            node = element.getParentNode();
        }
        throw new IllegalStateException("Bean: " + element.getNodeName() + " has no grandparent");
    }
}
