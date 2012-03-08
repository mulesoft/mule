/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class AbstractExpressionLanguageMap<K, V> implements Map<K, V>
{

    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException();
    }

    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> m)
    {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Collection<V> values()
    {
        throw new UnsupportedOperationException();
    }

    public int size()
    {
        return keySet().size();
    }

    public boolean isEmpty()
    {
        return keySet().isEmpty();
    }

}
