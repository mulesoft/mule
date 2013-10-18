/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.stax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public final class MapNamespaceContext implements NamespaceContext
{
    private Map<String, String> namespaces = new HashMap<String, String>();

    public MapNamespaceContext()
    {
        super();
    }

    public MapNamespaceContext(final Map<String, String> ns)
    {
        this();
        this.namespaces = ns;
    }

    public void addNamespace(final String prefix, final String namespaceURI)
    {
        this.namespaces.put(prefix, namespaceURI);
    }

    public void addNamespaces(final Map<String, String> ns)
    {
        this.namespaces.putAll(ns);
    }

    @Override
    public String getNamespaceURI(String prefix)
    {
        return namespaces.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI)
    {
        for (Map.Entry<String, String> entry : namespaces.entrySet())
        {
            if (entry.getValue().equals(namespaceURI))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI)
    {
        return null;
    }

    public Map<String, String> getUsedNamespaces()
    {
        return namespaces;
    }
}
