/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.util.CoreXMLUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

/**
 * All attributes from at least one set must be provided
 */
public class CheckRequiredAttributes implements PreProcessor
{

    // maps from attribute name to attribute set index (integer)
    private Map knownAttributes = new HashMap();
    // maps from attribute set index to number of attributes in that set
    private Map numberOfAttributes = new HashMap();
    // description of acceptable attributes
    private String summary;

    public CheckRequiredAttributes(String[][] attributeSets)
    {
        StringBuffer buffer = new StringBuffer();
        for (int set = 0; set < attributeSets.length; set++)
        {
            String[] attributes = attributeSets[set];
            // ignore empty sets
            if (attributes.length > 0)
            {
                Integer index = new Integer(set);
                numberOfAttributes.put(index, new Integer(attributes.length));
                if (set > 0)
                {
                    buffer.append("; ");
                }
                for (int attribute = 0; attribute < attributes.length; attribute++)
                {
                    knownAttributes.put(attributes[attribute], index);
                    if (attribute > 0)
                    {
                        buffer.append(", ");
                    }
                    buffer.append(attributes[attribute]);
                }
            }
        }
        summary = buffer.toString();
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        // map from attribute set index to count
        Map foundAttributesCount = new HashMap();

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String alias = CoreXMLUtils.attributeName((Attr) attributes.item(i));
            String name = null == config ? alias : config.translateName(alias);
            if (knownAttributes.containsKey(name))
            {
                Integer index = (Integer) knownAttributes.get(name);
                if (!foundAttributesCount.containsKey(index))
                {
                    foundAttributesCount.put(index, new Integer(0));
                }
                foundAttributesCount.put(index,
                        new Integer(1 + ((Integer) foundAttributesCount.get(index)).intValue()));

            }
        }

        boolean ok = false;
        Iterator indices = foundAttributesCount.keySet().iterator();
        while (indices.hasNext() && !ok)
        {
            Object index = indices.next();
            Object count = foundAttributesCount.get(index);
            ok = numberOfAttributes.get(index).equals(count);
        }
        if (!ok)
        {
            throw new CheckRequiredAttributesException(element, summary);
        }
    }

    public static class CheckRequiredAttributesException extends IllegalStateException
    {

        private CheckRequiredAttributesException(Element element, String summary)
        {
            super("Element " + CoreXMLUtils.elementToString(element) +
                    " must have all attributes for one of the sets: " + summary + ".");
        }

    }

}
