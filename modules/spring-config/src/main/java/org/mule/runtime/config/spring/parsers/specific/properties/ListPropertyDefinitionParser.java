/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.properties;

import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

import org.w3c.dom.Element;

/**
 * This allows a child element to extends a list of values, via an attribute, on a parent setter.
 * Typically it is used with
 * {@link org.mule.config.spring.parsers.specific.properties.ElementInNestedMapDefinitionParser}
 * whose setter is {@link org.mule.config.spring.parsers.assembly.MapEntryCombiner#VALUE}.
 */
public class ListPropertyDefinitionParser extends ParentDefinitionParser
{

    public ListPropertyDefinitionParser(String attribute)
    {
        this(MapEntryCombiner.VALUE, attribute);
    }

    /**
     * This method is to explain how things work.  If you need to call it, then you also need to replace
     * override the class ({#link #getBeanClass}).
     *
     * @param setter
     * @param attribute
     */
    protected ListPropertyDefinitionParser(String setter, String attribute)
    {
        setIgnoredDefault(true);
        removeIgnored(attribute);
        addCollection(attribute);
        if (!setter.equals(attribute))
        {
            addAlias(attribute, setter);
        }
    }

    protected Class getBeanClass(Element element)
    {
        return MapEntryCombiner.class;
    }

}
