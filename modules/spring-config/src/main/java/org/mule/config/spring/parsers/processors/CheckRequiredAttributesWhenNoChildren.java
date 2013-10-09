/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.w3c.dom.Element;

/**
 * All attributes from at least one set must be provided when there are no child
 * elements with the name specified present.
 */
public class CheckRequiredAttributesWhenNoChildren extends CheckRequiredAttributes
{
    private String elementName;

    public CheckRequiredAttributesWhenNoChildren(String[][] attributeNames, String elementName)
    {
        super(attributeNames);
        this.elementName = elementName;
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        // If there are child elements we skip this check
        if (element.getElementsByTagName(elementName).getLength() > 0)
        {
            return;
        }
        super.preProcess(config, element);
    }
}
