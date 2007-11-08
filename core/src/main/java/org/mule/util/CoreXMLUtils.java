/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;

/**
 * These only depend on standard (JSE) XML classes and are used by Spring config code.
 * For a more extensive (sub-)class, see the XMLUtils class in the XML module.
 */
public class CoreXMLUtils
{

    public static final String MULE_DEFAULT_NAMESPACE = "http://www.mulesource.org/schema/mule/core";
    public static final String MULE_NAMESPACE_PREFIX = "http://www.mulesource.org/schema/mule/";

    public static String elementToString(Element e)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(e.getTagName()).append("{");
        for (int i = 0; i < e.getAttributes().getLength(); i++)
        {
            if (i > 0)
            {
                buf.append(", ");
            }
            Node n = e.getAttributes().item(i);
            buf.append(attributeName((Attr) n)).append("=").append(n.getNodeValue());
        }
        buf.append("}");
        return buf.toString();
    }

    public static boolean isMuleNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.startsWith(MULE_NAMESPACE_PREFIX);
    }

    public static boolean isBeansNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI);
    }

    public static boolean isLocalName(Element element, String name)
    {
        return element.getLocalName().equals(name);
    }

    public static String attributeName(Attr attribute)
    {
        String name = attribute.getLocalName();
        if (null == name)
        {
            name = attribute.getName();
        }
        return name;
    }

}
