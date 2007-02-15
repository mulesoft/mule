/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.impl.MuleDescriptor;

import org.w3c.dom.Element;

/**
 * TODO
 */
public class ServiceDescriptorDefinitionParser extends AbstractChildBeanDefinitionParser
{

    protected Class getBeanClass(Element element)
    {
        return MuleDescriptor.class;
    }

    public boolean isCollection(Element element)
    {
        Element parent = (Element) element.getParentNode();
        if (parent.getNodeName().equals("beans"))
        {
            return false;
        }
        return true;
    }

    public String getPropertyName(Element e)
    {
        return "serviceDescriptor";
    }

}
