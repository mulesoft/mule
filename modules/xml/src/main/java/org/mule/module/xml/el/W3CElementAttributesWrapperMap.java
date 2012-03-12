/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.el;

import org.mule.el.mvel.DataConversion;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

public class W3CElementAttributesWrapperMap extends DataConversion implements Map<String, Object>
{
    private Element element;

    public W3CElementAttributesWrapperMap(Element element)
    {
        this.element = element;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object arg0)
    {
        return element.getAttributeNode((String) arg0) != null;
    }

    @Override
    public boolean containsValue(Object arg0)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(Object arg0)
    {
        if (!(arg0 instanceof String))
        {
            return null;
        }
        return element.getAttribute((String) arg0);
    }

    @Override
    public boolean isEmpty()
    {
        return !element.hasAttributes();
    }

    @Override
    public Set<String> keySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(String arg0, Object arg1)
    {
        synchronized (element)
        {
            String key = handleTypeCoercion(String.class, arg0);
            if (containsKey(arg0))
            {
                element.getAttributeNode(arg0).setTextContent(handleTypeCoercion(String.class, arg1));
            }
            else
            {
                element.setAttribute(key, handleTypeCoercion(String.class, arg1));
            }
        }
        return arg1;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> arg0)
    {
        for (Entry<? extends String, ? extends Object> entry : arg0.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String remove(Object arg0)
    {
        if (!(arg0 instanceof String))
        {
            return null;
        }
        else if (containsKey(arg0))
        {
            String attr = element.getAttribute((String) arg0);
            element.removeAttribute((String) arg0);
            return attr;
        }
        else
        {
            return null;
        }
    }

    @Override
    public int size()
    {
        return element.getAttributes().getLength();
    }

    @Override
    public Collection<Object> values()
    {
        throw new UnsupportedOperationException();
    }

}
