/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.api.util.Preconditions;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// @ThreadSafe
public class ArrayUtils extends org.apache.commons.lang3.ArrayUtils {

  /**
   * Like {@link #toString(Object)} but considers at most <code>maxElements</code> values; overflow is indicated by an appended
   * "[..]" ellipsis.
   */
  public static String toString(Object array, int maxElements) {
    String result;

    Class componentType = array.getClass().getComponentType();
    if (Object.class.isAssignableFrom(componentType)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((Object[]) array, 0, maxElements)));
    } else if (componentType.equals(Boolean.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((boolean[]) array, 0, maxElements)));
    } else if (componentType.equals(Byte.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((byte[]) array, 0, maxElements)));
    } else if (componentType.equals(Character.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((char[]) array, 0, maxElements)));
    } else if (componentType.equals(Short.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((short[]) array, 0, maxElements)));
    } else if (componentType.equals(Integer.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((int[]) array, 0, maxElements)));
    } else if (componentType.equals(Long.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((long[]) array, 0, maxElements)));
    } else if (componentType.equals(Float.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((float[]) array, 0, maxElements)));
    } else if (componentType.equals(Double.TYPE)) {
      result = ArrayUtils.toString((ArrayUtils.subarray((double[]) array, 0, maxElements)));
    } else {
      throw new IllegalArgumentException("Unknown array service type: " + componentType.getName());
    }

    if (Array.getLength(array) > maxElements) {
      StringBuilder buf = new StringBuilder(result);
      buf.insert(buf.length() - 1, " [..]");
      result = buf.toString();
    }

    return result;

  }

  /**
   * Creates a copy of the given array, but with the given <code>Class</code> as element type. Useful for arrays of objects that
   * implement multiple interfaces and a "typed view" onto these objects is required.
   *
   * @param objects the array of objects
   * @param clazz the desired service type of the new array
   * @return <code>null</code> when objects is <code>null</code>, or a new array containing the elements of the source array which
   *         is typed to the given <code>clazz</code> parameter. If <code>clazz</code> is already the service type of the source
   *         array, the source array is returned (i.e. no copy is created).
   * @throws IllegalArgumentException if the <code>clazz</code> argument is <code>null</code>.
   * @throws ArrayStoreException if the elements in <code>objects</code> cannot be cast to <code>clazz</code>.
   */
  public static Object[] toArrayOfComponentType(Object[] objects, Class clazz) {
    if (objects == null || objects.getClass().getComponentType().equals(clazz)) {
      return objects;
    }

    if (clazz == null) {
      throw new IllegalArgumentException("Array target class must not be null");
    }

    Object[] result = (Object[]) Array.newInstance(clazz, objects.length);
    System.arraycopy(objects, 0, result, 0, objects.length);
    return result;
  }

  public static Object[] setDifference(Object[] a, Object[] b) {
    Collection aCollecn = new HashSet(Arrays.asList(a));
    Collection bCollecn = Arrays.asList(b);
    aCollecn.removeAll(bCollecn);
    return aCollecn.toArray();
  }

  public static String[] setDifference(String[] a, String[] b) {
    Object[] ugly = setDifference((Object[]) a, b);
    String[] copy = new String[ugly.length];
    System.arraycopy(ugly, 0, copy, 0, ugly.length);
    return copy;
  }

  /**
   * Calculates the intersection between two arrays, as if they were sets.
   *
   * @return A new array with the intersection.
   */
  public static String[] intersection(String[] a, String[] b) {
    Set<String> result = new HashSet<String>();
    result.addAll(Arrays.asList(a));
    result.retainAll(Arrays.asList(b));
    return result.toArray(new String[result.size()]);
  }

  public static int getLength(Object array) {
    if (array == null) {
      return 0;
    }

    Preconditions.checkArgument(array.getClass().isArray(),
                                String.format("Object of type %s is not an array", array.getClass().getName()));

    Class<?> componentType = array.getClass().getComponentType();
    if (!componentType.isPrimitive()) {
      return ((Object[]) array).length;
    } else if (componentType.equals(Boolean.TYPE)) {
      return ((boolean[]) array).length;
    } else if (componentType.equals(Byte.TYPE)) {
      return ((byte[]) array).length;
    } else if (componentType.equals(Character.TYPE)) {
      return ((char[]) array).length;
    } else if (componentType.equals(Short.TYPE)) {
      return ((short[]) array).length;
    } else if (componentType.equals(Integer.TYPE)) {
      return ((int[]) array).length;
    } else if (componentType.equals(Long.TYPE)) {
      return ((long[]) array).length;
    } else if (componentType.equals(Float.TYPE)) {
      return ((float[]) array).length;
    } else if (componentType.equals(Double.TYPE)) {
      return ((double[]) array).length;
    } else {
      throw new IllegalArgumentException("Cannot determine length for array of " + componentType.getName());
    }
  }

}
