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
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Attributes from two different sets cannot appear together
 */
public class CheckExclusiveAttributes implements PreProcessor
{
    private Collection<AttributeSet> attributeSets;

    public CheckExclusiveAttributes(String[][] attributeNames)
    {
        super();
        
        attributeSets = new ArrayList<AttributeSet>();
        for (int i = 0; i < attributeNames.length; i++)
        {
            String[] attributes = attributeNames[i];
            attributeSets.add(new AttributeSet(attributes));
        }            
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        Collection<AttributeSet> allMatchingSets = new ArrayList<AttributeSet>(attributeSets);
        boolean atLeastOneAttributeDidMatch = false;

        // itereate over all attribute names in 'element'
        NamedNodeMap attributes = element.getAttributes();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++)
        {
            String alias = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            if (isOptionalAttribute(alias))
            {
                continue;
            }

            allMatchingSets = filterMatchingSets(allMatchingSets, alias);
            atLeastOneAttributeDidMatch = true;
        }
        
        if (atLeastOneAttributeDidMatch && allMatchingSets.size() == 0)
        {
            CheckExclusiveAttributesException ex = 
                CheckExclusiveAttributesException.createForDisjunctGroups(element, attributeSets);
          throw ex;

        }
        else if (atLeastOneAttributeDidMatch && allMatchingSets.size() > 1)
        {
            CheckExclusiveAttributesException ex = 
                CheckExclusiveAttributesException.createForInsufficientAttributes(element, allMatchingSets);
            throw ex;
        }
    }

    private Collection<AttributeSet> filterMatchingSets(Collection<AttributeSet> allMatchingSets, 
        String attributeName)
    {
        Collection<AttributeSet> newMatchingSets = new ArrayList<AttributeSet>();
        for (AttributeSet currentSet : allMatchingSets)
        {
            if (currentSet.containsAttribute(attributeName))
            {
                newMatchingSets.add(currentSet);
            }
        }
        return newMatchingSets;
    }

    private boolean isOptionalAttribute(String name)
    {
        return findMatchingAttributeSets(name).size() == 0;
    }

    private Collection<AttributeSet> findMatchingAttributeSets(String alias)
    {
        List<AttributeSet> matchingSets = new ArrayList<AttributeSet>();
        for (AttributeSet currentSet : attributeSets)
        {
            if (currentSet.containsAttribute(alias))
            {
                matchingSets.add(currentSet);
            }
        }
        return matchingSets;
    }

    private static class AttributeSet
    {
        private String[] attributeNames;

        public AttributeSet(String[] attributeNames)
        {
            super();
            this.attributeNames = attributeNames;
        }

        public boolean containsAttribute(String name)
        {
            for (int i = 0; i < attributeNames.length; i++)
            {
                String element = attributeNames[i];
                if (element.equals(name))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString()
        {
            return Arrays.toString(attributeNames);
        }
    }
    
    public static class CheckExclusiveAttributesException extends IllegalStateException
    {
        public static CheckExclusiveAttributesException createForDisjunctGroups(Element element,
            Collection<AttributeSet> allMatchingSets)
        {
            String message = createMessage(element, allMatchingSets);
            return new CheckExclusiveAttributesException(message);
        }

        private static String createMessage(Element element, Collection<AttributeSet> allMatchingSets)
        {
            StringBuilder buf = new StringBuilder("The attributes of Element ");
            buf.append(SpringXMLUtils.elementToString(element));
            buf.append(" do not match the exclusive groups");
            
            for (AttributeSet match : allMatchingSets)
            {
                buf.append(" ");
                buf.append(match.toString());
            }
            
            return buf.toString();
        }
        
        public static CheckExclusiveAttributesException createForInsufficientAttributes(Element element,
            Collection<AttributeSet> attributeSets)
        {
            StringBuilder buf = new StringBuilder("Attributes of Element ");
            buf.append(SpringXMLUtils.elementToString(element));
            buf.append(" do not satisfy the exclusive groups");
            
            for (AttributeSet attributeSet : attributeSets)
            {
                buf.append(" ");
                buf.append(attributeSet);
            }
            buf.append(".");
            
            return new CheckExclusiveAttributesException(buf.toString());
        }
        
        private CheckExclusiveAttributesException(String message)
        {
            super(message);
        }
    }
}
