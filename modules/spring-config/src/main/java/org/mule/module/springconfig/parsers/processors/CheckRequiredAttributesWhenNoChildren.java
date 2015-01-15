/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.processors;

import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;

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
