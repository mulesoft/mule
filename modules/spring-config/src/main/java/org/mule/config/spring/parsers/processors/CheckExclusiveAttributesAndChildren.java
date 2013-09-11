/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

/**
 * Attributes and children elements cannot appear together. Child names are either
 * node names or types.
 */
public class CheckExclusiveAttributesAndChildren implements PreProcessor
{
    private final static Pattern TYPE_REGEXP = Pattern.compile("\\{(.*)\\}(.*)");

    private final Set<String> attributeNames;
    private final Set<String> childrenNames;
    private final Set<ChildType> childrenTypes;

    private static class ChildType
    {
        String ns;
        String name;

        public ChildType(String ns, String name)
        {
            this.ns = ns;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return "{" + ns + "}" + name;
        }

    }

    public CheckExclusiveAttributesAndChildren(String[] attributeNames, String[] childrenNamesOrTypes)
    {
        this.attributeNames = new HashSet<String>(Arrays.asList(attributeNames));
        this.childrenNames = new HashSet<String>();
        this.childrenTypes = new HashSet<ChildType>();
        parseChildrenNamesOrTypes(childrenNamesOrTypes);
    }

    private void parseChildrenNamesOrTypes(String[] childrenNamesOrTypes)
    {
        for (final String childrenNameOrType : childrenNamesOrTypes)
        {
            final Matcher matcher = TYPE_REGEXP.matcher(childrenNameOrType);
            if (matcher.matches())
            {
                childrenTypes.add(new ChildType(matcher.group(1), matcher.group(2)));
            }
            else
            {
                childrenNames.add(childrenNameOrType);
            }
        }
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
            checkChildNode(element, attributeName, childNodes.item(j));
        }
    }

    private void checkChildNode(Element element, final String attributeName, final Node child)
    {
        if (child.getNodeType() == Node.ELEMENT_NODE)
        {
            checkChildElement(element, attributeName, (Element) child);
        }
    }

    private void checkChildElement(Element element, final String attributeName, final Element child)
    {
        checkAttributeNameMatch(element, attributeName, child);

        for (final ChildType childrenType : childrenTypes)
        {
            final TypeInfo typeInfo = (TypeInfo) child;

            if (((childrenType.ns.equals(typeInfo.getTypeNamespace()) && childrenType.name.equals(typeInfo.getTypeName())))
                || typeInfo.isDerivedFrom(childrenType.ns, childrenType.name, TypeInfo.DERIVATION_EXTENSION))
            {

                throw new CheckExclusiveAttributesAndChildrenException(
                    "Element " + SpringXMLUtils.elementToString(element) + " can't contain child of type "
                                    + childrenType + " because it defines attribute " + attributeName);
            }
        }

    }

    private void checkAttributeNameMatch(Element element, final String attributeName, final Element child)
    {
        final String childElementName = child.getLocalName();

        if (childrenNames.contains(childElementName))
        {
            throw new CheckExclusiveAttributesAndChildrenException("Element "
                                                                   + SpringXMLUtils.elementToString(element)
                                                                   + " can't contain child "
                                                                   + childElementName
                                                                   + " because it defines attribute "
                                                                   + attributeName);
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
