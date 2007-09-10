/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class BadAttributeFilter extends AbstractElementBasedFilter
{

    private Set badAttributes;

    public BadAttributeFilter(String[] badAttributes, MuleDefinitionParser delegate)
    {
        super(delegate);
        this.badAttributes = new HashSet(Arrays.asList(badAttributes));
    }

    public boolean accept(Element element)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            if (badAttributes.contains(attributes.item(i).getLocalName()))
            {
                return false;
            }
        }
        return true;
    }

}
