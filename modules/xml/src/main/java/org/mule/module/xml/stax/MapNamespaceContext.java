/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.stax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

public final class MapNamespaceContext implements NamespaceContext
{
    private Map namespaces = new HashMap();

    public MapNamespaceContext()
    {
        super();
    }

    public MapNamespaceContext(final Map ns)
    {
        this();
        this.namespaces = ns;
    }

    public void addNamespace(final String prefix, final String namespaceURI)
    {
        this.namespaces.put(prefix, namespaceURI);
    }

    public void addNamespaces(final Map ns)
    {
        this.namespaces.putAll(ns);
    }

    public String getNamespaceURI(String prefix)
    {
        return (String) namespaces.get(prefix);
    }

    public String getPrefix(String namespaceURI)
    {
        for (Iterator itr = namespaces.entrySet().iterator(); itr.hasNext();)
        {
            Map.Entry e = (Map.Entry) itr.next();
            if (e.getValue().equals(namespaceURI))
            {
                return (String) e.getKey();
            }
        }
        return null;
    }

    public Iterator getPrefixes(String namespaceURI)
    {
        return null;
    }

    public Map getUsedNamespaces()
    {
        return namespaces;
    }
}
