/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.util;

import java.lang.reflect.Array;

public class ArrayUtils extends org.apache.commons.lang.ArrayUtils
{

    /**
     * TODO document me :)
     * 
     * @param objects
     * @param clazz
     * @return
     */
    public static Object[] toArrayOfComponentType(Object[] objects, Class clazz)
    {
        if (objects == null || objects.getClass().getComponentType().equals(clazz)) {
            return objects;
        }

        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null!");
        }

        Object[] result = (Object[])Array.newInstance(clazz, objects.length);
        System.arraycopy(objects, 0, result, 0, objects.length);
        return result;
    }

}
