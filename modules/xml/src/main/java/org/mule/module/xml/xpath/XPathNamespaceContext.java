/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.xpath;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import net.sf.saxon.lib.NamespaceConstant;

/**
 * Implementation of {@link NamespaceContext} which can be parametrized
 * through a {@link Map} in which the keys are Strings with the custom
 * namespace prefixes and the values are those namespaces' URIs as Strings.
 * <p/>
 * Besides the namespaces parametrized, this implementation also recognizes.
 * the default XML namespaces (xml, xs, and xsi) and their default URIs. Notice
 * that if the parametrization map contains custom URIs for any of those prefixes,
 * then the parametrized ones will take precedence over the default ones
 *
 * @since 3.6.0
 */
final class XPathNamespaceContext implements NamespaceContext
{

    private final BiMap<String, String> prefixToNamespaceMap;

    XPathNamespaceContext(Map<String, String> prefixToNamespaceMap)
    {
        Map<String, String> namespaces = new HashMap<>();
        if (prefixToNamespaceMap != null)
        {
            namespaces.putAll(prefixToNamespaceMap);
        }

        loadDefaultNamespaces(namespaces);
        this.prefixToNamespaceMap = ImmutableBiMap.copyOf(namespaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespaceURI(String prefix)
    {
        if (prefix == null)
        {
            throw new IllegalArgumentException("prefix cannot be null");
        }

        return prefixToNamespaceMap.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix(String namespaceURI)
    {
        return prefixToNamespaceMap.inverse().get(namespaceURI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator getPrefixes(String namespaceURI)
    {
        return ImmutableList.<String>builder()
                .addAll(prefixToNamespaceMap.inverse().keySet())
                .build()
                .iterator();
    }

    private void loadDefaultNamespaces(Map<String, String> namespaces)
    {
        putIfAbsent(namespaces, XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
        putIfAbsent(namespaces, "xml", NamespaceConstant.XML);
        putIfAbsent(namespaces, "xs", NamespaceConstant.SCHEMA);
        putIfAbsent(namespaces, "xsi", NamespaceConstant.SCHEMA_INSTANCE);
    }

    private void putIfAbsent(Map<String, String> map, String key, String value)
    {
        if (!map.containsKey(key))
        {
            map.put(key, value);
        }
    }
}
