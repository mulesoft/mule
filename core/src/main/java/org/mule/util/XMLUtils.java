/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * These only depend on standard (JSE) XML classes and are used by Spring config code.
 * For a more extensive (sub-)class, see the XMLUtils class in the XML module.
 */
public class XMLUtils
{

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

    public static String getTextChild(Element element)
    {
        NodeList children = element.getChildNodes();
        String value = null;
        for (int i = 0; i < children.getLength(); ++i)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE)
            {
                if (null != value)
                {
                    throw new IllegalStateException(
                            "Element " + elementToString(element) + " has more than one text child.");
                }
                else
                {
                    value = child.getNodeValue();
                }
            }
        }
        return value;
    }

}
