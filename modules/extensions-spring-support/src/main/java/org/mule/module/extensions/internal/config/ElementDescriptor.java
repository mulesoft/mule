/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import org.mule.util.CollectionUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * A descriptor class which allows decoupling the running code
 * from the actual XML element being parsed. This is convenient not only
 * for testing purposes, but also to provide an intermediate stage
 * in which placeholders and other parsing pre-processors can be
 * executed before it reaches the actual parsers
 *
 * @since 3.7.0
 */
public final class ElementDescriptor
{

    private final String name;
    private final Map<String, String> attributes;
    private final Multimap<String, ElementDescriptor> childs;

    public ElementDescriptor(String name, Map<String, String> attributes, List<ElementDescriptor> childs)
    {
        this.name = name;
        this.attributes = attributes;
        this.childs = ArrayListMultimap.create();
        for (ElementDescriptor child : childs)
        {
            this.childs.put(child.getName(), child);
        }
    }

    public String getName()
    {
        return name;
    }

    public boolean hasAttribute(String attributeName)
    {
        return !StringUtils.isBlank(getAttribute(attributeName));
    }

    public String getAttribute(String attributeName)
    {
        return attributes.get(attributeName);
    }

    public Collection<ElementDescriptor> getChilds()
    {
        return childs.values();
    }

    public ElementDescriptor getChildByName(String childName)
    {
        Collection<ElementDescriptor> values = childs.get(childName);
        return CollectionUtils.isEmpty(values) ? null : values.iterator().next();
    }

    public Collection<ElementDescriptor> getChildsByName(String childName)
    {
        return childs.get(childName);
    }
}
