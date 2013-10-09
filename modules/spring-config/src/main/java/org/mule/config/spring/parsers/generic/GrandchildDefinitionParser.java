/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.generic;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Same as ChildDefinitionParser but injects the child element into the grandparent object 
 * (2 levels up in the XML tree).
 */
public class GrandchildDefinitionParser extends ChildDefinitionParser
{
    public GrandchildDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }

    public GrandchildDefinitionParser(String setterMethod, Class clazz, Class constraint, boolean allowClassAttribute)
    {
        super(setterMethod, clazz, constraint, allowClassAttribute);
    }

    @Override
    protected String getParentBeanName(Element element)
    {
        return getGrandparentBeanName(element);
    }

    protected String getGrandparentBeanName(Element element)
    {
        Node parent = element.getParentNode();
        if (parent == null)
        {
            logger.error("No parent node found for element " + element);
            return null;
        }
        Node grandparent = parent.getParentNode();
        if (grandparent == null)
        {
            logger.error("No parent node found for element " + parent);
            return null;
        }
        Node grandparentNameAttribute = grandparent.getAttributes().getNamedItem("name");
        if (grandparentNameAttribute == null)
        {
            logger.error("Grandparent node has no 'name' attribute: " + grandparent);
            return null;
        }
        return grandparentNameAttribute.getNodeValue();
    }
}
