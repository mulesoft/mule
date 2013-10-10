/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * If this attribute is present, no other can be
 */
public class CheckExclusiveAttributeAndText implements PreProcessor
{

    public static final int NONE = -1;
    private String attribute;

    public CheckExclusiveAttributeAndText(String attribute)
    {
        this.attribute = attribute;
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        List foundAttributes = new LinkedList();
        boolean found = false;

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            if (!config.isIgnored(alias))
            {
                if (attribute.equals(alias))
                {
                    found = true;
                }
                else
                {
                    foundAttributes.add(alias);
                }
            }
        }

        if (found && !element.getTextContent().isEmpty())
        {
            StringBuffer message = new StringBuffer("The attribute '");
            message.append(attribute);
            message.append("' cannot be used with text content");
            message.append(" in element ");
            message.append(SpringXMLUtils.elementToString(element));
            message.append(".");
            throw new CheckExclusiveAttributeException(message.toString());
        }
    }

    public static class CheckExclusiveAttributeException extends IllegalStateException
    {

        private CheckExclusiveAttributeException(String message)
        {
            super(message);
        }

    }

}
