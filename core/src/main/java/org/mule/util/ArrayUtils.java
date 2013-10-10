/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

// @ThreadSafe
public class ArrayUtils extends org.apache.commons.lang.ArrayUtils
{

    /**
     * Like {@link #toString(Object)} but considers at most <code>maxElements</code>
     * values; overflow is indicated by an appended "[..]" ellipsis.
     */
    public static String toString(Object array, int maxElements)
    {
        String result;

        Class componentType = array.getClass().getComponentType();
        if (Object.class.isAssignableFrom(componentType))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((Object[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Boolean.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((boolean[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Byte.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((byte[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Character.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((char[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Short.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((short[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Integer.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((int[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Long.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((long[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Float.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((float[]) array, 0, maxElements)));
        }
        else if (componentType.equals(Double.TYPE))
        {
            result = ArrayUtils.toString((ArrayUtils.subarray((double[]) array, 0, maxElements)));
        }
        else
        {
            throw new IllegalArgumentException("Unknown array service type: " + componentType.getName());
        }

        if (Array.getLength(array) > maxElements)
        {
            StringBuffer buf = new StringBuffer(result);
            buf.insert(buf.length() - 1, " [..]");
            result = buf.toString();
        }

        return result;

    }

    /**
     * Creates a copy of the given array, but with the given <code>Class</code> as
     * element type. Useful for arrays of objects that implement multiple interfaces
     * and a "typed view" onto these objects is required.
     * 
     * @param objects the array of objects
     * @param clazz the desired service type of the new array
     * @return <code>null</code> when objects is <code>null</code>, or a new
     *         array containing the elements of the source array which is typed to
     *         the given <code>clazz</code> parameter. If <code>clazz</code> is
     *         already the service type of the source array, the source array is
     *         returned (i.e. no copy is created).
     * @throws IllegalArgumentException if the <code>clazz</code> argument is
     *             <code>null</code>.
     * @throws ArrayStoreException if the elements in <code>objects</code> cannot
     *             be cast to <code>clazz</code>.
     */
    public static Object[] toArrayOfComponentType(Object[] objects, Class clazz)
    {
        if (objects == null || objects.getClass().getComponentType().equals(clazz))
        {
            return objects;
        }

        if (clazz == null)
        {
            throw new IllegalArgumentException("Array target class must not be null");
        }

        Object[] result = (Object[]) Array.newInstance(clazz, objects.length);
        System.arraycopy(objects, 0, result, 0, objects.length);
        return result;
    }

    public static Object[] setDifference(Object[] a, Object[] b)
    {
        Collection aCollecn = new HashSet(Arrays.asList(a));
        Collection bCollecn = Arrays.asList(b);
        aCollecn.removeAll(bCollecn);
        return aCollecn.toArray();
    }

    public static String[] setDifference(String[] a, String[] b)
    {
        Object[] ugly = setDifference((Object[])a, b);
        String[] copy = new String[ugly.length];
        System.arraycopy(ugly, 0, copy, 0, ugly.length);
        return copy;
    }
    
}
