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

import org.dom4j.Attribute;
import org.dom4j.Element;

public class DOM4JElementAttributesWrapperMap extends DataConversion implements Map<String, Object>
{
    private Element element;

    public DOM4JElementAttributesWrapperMap(Element element)
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
        return element.attribute((String) arg0) != null;
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
        return element.attributeValue((String) arg0);
    }

    @Override
    public boolean isEmpty()
    {
        return element.attributeCount() == 0;
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
                element.attribute(key).setText(handleTypeCoercion(String.class, arg1));
            }
            else
            {
                element.addAttribute(key, handleTypeCoercion(String.class, arg1));
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
            Attribute attr = element.attribute((String) arg0);
            String value = attr.getText();
            element.remove(attr);
            return value;
        }
        else
        {
            return null;
        }
    }

    @Override
    public int size()
    {
        return element.attributeCount();
    }

    @Override
    public Collection<Object> values()
    {
        throw new UnsupportedOperationException();
    }

}
