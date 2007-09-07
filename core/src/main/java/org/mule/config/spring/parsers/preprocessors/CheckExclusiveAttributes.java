/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.preprocessors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class CheckExclusiveAttributes implements PreProcessor
{

    public static final int NONE = -1;
    private Map knownAttributes = new HashMap();

    public CheckExclusiveAttributes(String[][] attributeSets)
    {
        for (int set = 0; set < attributeSets.length; set++)
        {
            String[] attributes = attributeSets[set];
            for (int attribute = 0; attribute < attributes.length; attribute++)
            {
                knownAttributes.put(attributes[attribute], new Integer(set));
            }
        }
    }

    public void preProcess(Element element)
    {
        List foundAttributes = new LinkedList();
        int foundSetIndex = NONE;

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String name = attributes.item(i).getLocalName();
            if (knownAttributes.containsKey(name))
            {
                int index = ((Integer) knownAttributes.get(name)).intValue();
                if (foundSetIndex != NONE && foundSetIndex != index)
                {
                    StringBuffer message = new StringBuffer("The attribute '");
                    message.append(name);
                    message.append("' cannot appear with the attribute");
                    if (foundAttributes.size() > 1)
                    {
                        message.append("s");
                    }
                    Iterator found = foundAttributes.iterator();
                    while (found.hasNext())
                    {
                        message.append(" '");
                        message.append(found.next());
                        message.append("'");
                    }
                    message.append(" in element ");
                    message.append(MuleHierarchicalBeanDefinitionParserDelegate.elementToString(element));
                    message.append(".");
                    throw new IllegalStateException(message.toString());
                }
                else
                {
                    foundSetIndex = index;
                    foundAttributes.add(name);
                }
            }
        }
    }

}
