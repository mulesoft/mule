/*
 * $Id:CheckExclusiveAttributes.java 8321 2007-09-10 19:22:52Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Attributes and children elements cannot appear together
 */
public class CheckExclusiveAttributesAndChildren implements PreProcessor
{
    private final Set<String> attributeNames;
    private final Set<String> childrenNames;

    public CheckExclusiveAttributesAndChildren(String[] attributeNames, String[] childrenNames)
    {
        this.attributeNames = new HashSet<String>(Arrays.asList(attributeNames));
        this.childrenNames = new HashSet<String>(Arrays.asList(childrenNames));
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        final NamedNodeMap attributes = element.getAttributes();
        final int attributesCount = attributes.getLength();

        for (int i = 0; i < attributesCount; i++)
        {
            final String attributeName = SpringXMLUtils.attributeName((Attr) attributes.item(i));

            if (attributeNames.contains(attributeName))
            {
                ensureNoForbiddenChildren(element, attributeName);
            }
        }
    }

    private void ensureNoForbiddenChildren(Element element, final String attributeName)
    {
        final NodeList childNodes = element.getChildNodes();
        final int childNodesCount = childNodes.getLength();
        for (int j = 0; j < childNodesCount; j++)
        {
            final String childElementName = childNodes.item(j).getLocalName();
            if (childrenNames.contains(childElementName))
            {
                throw new CheckExclusiveAttributesAndChildrenException(
                    "Element " + SpringXMLUtils.elementToString(element) + " can't contain child "
                                    + childElementName + " because it defines attribute "
                                    + attributeName);
            }
        }
    }

    public static class CheckExclusiveAttributesAndChildrenException extends IllegalStateException
    {
        private static final long serialVersionUID = 8661524219979354246L;

        public CheckExclusiveAttributesAndChildrenException(String message)
        {
            super(message);
        }
    }
}
