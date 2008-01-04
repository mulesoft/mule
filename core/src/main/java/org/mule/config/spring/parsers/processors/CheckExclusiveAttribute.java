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

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.util.CoreXMLUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

/**
 * If this attribute is present, no other can be
 */
public class CheckExclusiveAttribute implements PreProcessor
{

    public static final int NONE = -1;
    private String attribute;

    public CheckExclusiveAttribute(String attribute)
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
            String alias = CoreXMLUtils.attributeName((Attr) attributes.item(i));
            if (! config.isIgnored(alias))
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

        if (found && foundAttributes.size() > 0)
        {
            StringBuffer message = new StringBuffer("The attribute '");
            message.append(attribute);
            message.append("' cannot appear with the attribute");
            if (foundAttributes.size() > 1)
            {
                message.append("s");
            }
            Iterator others = foundAttributes.iterator();
            while (others.hasNext())
            {
                message.append(" '");
                message.append(others.next());
                message.append("'");
            }
            message.append(" in element ");
            message.append(CoreXMLUtils.elementToString(element));
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