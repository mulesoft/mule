/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

// @Immutable
public class MapUtils extends org.apache.commons.collections.MapUtils
{

    /**
     * Convenience method for CollectionUtil#mapWithKeysAndValues(Class, Iterator,
     * Iterator); keys and values can be null or empty.
     */
    public static Map mapWithKeysAndValues(Class mapClass, Object[] keys, Object[] values)
    {
        Collection keyCollection = (keys != null ? Arrays.asList(keys) : Collections.EMPTY_LIST);
        Collection valuesCollection = (values != null ? Arrays.asList(values) : Collections.EMPTY_LIST);
        return mapWithKeysAndValues(mapClass, keyCollection.iterator(), valuesCollection.iterator());
    }

    /**
     * Convenience method for CollectionUtil#mapWithKeysAndValues(Class, Iterator,
     * Iterator); keys and values can be null or empty.
     */
    public static Map mapWithKeysAndValues(Class mapClass, Collection keys, Collection values)
    {
        keys = (keys != null ? keys : Collections.EMPTY_LIST);
        values = (values != null ? values : Collections.EMPTY_LIST);
        return mapWithKeysAndValues(mapClass, keys.iterator(), values.iterator());
    }

    /**
     * Create & populate a Map of arbitrary class. Populating stops when either the
     * keys or values iterator is null or exhausted.
     * 
     * @param mapClass the Class of the Map to instantiate
     * @param keys iterator for Objects ued as keys
     * @param values iterator for Objects used as values
     * @return the instantiated Map
     */
    public static Map mapWithKeysAndValues(Class mapClass, Iterator keys, Iterator values)
    {
        Map m = null;

        if (mapClass == null)
        {
            throw new IllegalArgumentException("Map class must not be null!");
        }

        try
        {
            m = (Map)mapClass.newInstance();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        if (keys != null && values != null)
        {
            while (keys.hasNext() && values.hasNext())
            {
                m.put(keys.next(), values.next());
            }
        }

        return m;
    }

}
