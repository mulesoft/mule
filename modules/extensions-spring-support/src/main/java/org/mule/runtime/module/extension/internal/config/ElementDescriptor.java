/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import org.mule.runtime.core.util.CollectionUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

/**
 * A descriptor which allows decoupling the running code
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
    private final Element sourceElement;

    /**
     * Creates a new instance which describes an XML element
     * which state is represented by the passed arguments
     *
     * @param name          the name of the element
     * @param attributes    a {@link Map} containing the element's attribute names and their values
     * @param childs        a {@link List} of other {@link ElementDescriptor} instances which represents this
     *                      element's childs. Can be empty but must not be {@code null}
     * @param sourceElement the {@link Element} from which {@code this} instance was derived
     */
    public ElementDescriptor(String name, Map<String, String> attributes, List<ElementDescriptor> childs, Element sourceElement)
    {
        this.name = name;
        this.attributes = attributes;
        this.childs = ArrayListMultimap.create();
        childs.forEach(child -> this.childs.put(child.getName(), child));
        this.sourceElement = sourceElement;
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

    public ElementDescriptor getChildByName(String childName)
    {
        Collection<ElementDescriptor> values = childs.get(childName);
        return CollectionUtils.isEmpty(values) ? null : values.iterator().next();
    }

    public Collection<ElementDescriptor> getChildsByName(String childName)
    {
        return childs.get(childName);
    }

    public Element getSourceElement()
    {
        return sourceElement;
    }
}
