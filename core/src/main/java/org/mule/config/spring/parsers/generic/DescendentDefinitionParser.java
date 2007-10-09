/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.util.StringUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An extension to {@line ChildDefinitionParser} which recurses up the DOM
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
