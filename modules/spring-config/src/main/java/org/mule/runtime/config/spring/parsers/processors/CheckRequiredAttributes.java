/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.util.SpringXMLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * All attributes from at least one set must be provided
 */
public class CheckRequiredAttributes implements PreProcessor
{
    Collection<List<String>> attributeSets;
    
    public CheckRequiredAttributes(String[][] attributeNames)
    {
        super();

        attributeSets = new ArrayList<List<String>>();
        for (int i = 0; i < attributeNames.length; i++)
        {
            String[] currentSet = attributeNames[i];
            if (currentSet.length > 0)
            {
                List<String> list = Arrays.asList(currentSet);
                attributeSets.add(list);
            }
        }
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        for (List<String> currentSet : attributeSets)
        {
            if (containsAllRequiredAttributes(currentSet, element))
            {
                return;
            }
        }
        
        throw new CheckRequiredAttributesException(element, attributeSets);
    }

    private boolean containsAllRequiredAttributes(List<String> currentSet, Element element)
    {
        Set<String> attributes = collectAttributes(element);
        if (attributes.size() == 0)
        {
            return false;
        }
        
        // Clone the set of attribute names and subtract all the element's attribute names from it.
        // If the remaining set is empty, all required attributes of this set were present.
        Set<String> remainingElementNames = new HashSet<String>(currentSet);
        remainingElementNames.removeAll(attributes);
        return (remainingElementNames.size() == 0);
    }
    
    private Set<String> collectAttributes(Element element)
    {
        Set<String> attributeNames = new HashSet<String>();
        
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            attributeNames.add(alias);
        }
        
        return attributeNames;
    }

    public static class CheckRequiredAttributesException extends IllegalStateException
    {
        private static String summary(Collection<List<String>> attributeSets)
        {
            StringBuilder buf = new StringBuilder();
            for (List<String> set : attributeSets)
            {
                if (buf.length() > 0)
                {
                    buf.append(" ");
                }
                
                if (set.isEmpty())
                {
                    continue;
                }
                
                buf.append(set.toString());
            }
            return buf.toString();
        }

        private CheckRequiredAttributesException(Element element, Collection<List<String>> attributeSets)
        {
            super("Element " + SpringXMLUtils.elementToString(element) +
                    " must have all attributes for one of the sets: " + summary(attributeSets) + ".");
        }
    }
}
