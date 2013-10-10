/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
